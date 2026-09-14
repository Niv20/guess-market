package guessmarket.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import guessmarket.dto.CloseEventRequestDto;
import guessmarket.dto.CommissionType;
import guessmarket.dto.CreateEventRequestDto;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.OpenEventRequestDto;
import guessmarket.dto.OrderSide;
import guessmarket.dto.PurchaseRequestDto;
import guessmarket.dto.SubmitOrderRequestDto;
import guessmarket.dto.TradingMethod;
import guessmarket.dto.network.MessageDto;
import guessmarket.dto.network.QuoteDto;
import guessmarket.engine.exception.EventAlreadyStartedException;
import guessmarket.engine.exception.EventNotActiveException;
import guessmarket.engine.exception.EventNotFoundException;
import guessmarket.engine.exception.EventNotStartedException;
import guessmarket.engine.exception.GuessMarketException;
import guessmarket.engine.exception.InvalidFileContentException;
import guessmarket.engine.exception.NotMarketMakerException;
import guessmarket.engine.exception.UserBlockedException;
import guessmarket.engine.exception.UserNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Locale;

/**
 * JSON endpoints translating session-bound requests to engine commands. Native Servlet multipart
 * stays in RAM: the threshold covers the entire permitted request and Part.write is never used.
 */
@WebServlet(name = "GuessMarketApi", urlPatterns = "/api/*", loadOnStartup = 1)
@MultipartConfig(fileSizeThreshold = 2_162_688, maxFileSize = 2_097_152, maxRequestSize = 2_162_688)
public final class MarketServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int MAX_FILE_BYTES = 2_097_152;
    private static final Gson JSON = new GsonBuilder().create();
    private ApplicationService application;

    @Override
    public void init() throws ServletException {
        Object service = getServletContext().getAttribute(ApplicationService.CONTEXT_ATTRIBUTE);
        if (!(service instanceof ApplicationService market)) {
            throw new ServletException("The market application has not been initialized.");
        }
        application = market;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        try {
            Object result = dispatch(request, response);
            write(response, result);
        } catch (ApiException failure) {
            fail(response, failure.status(), failure.getMessage());
        } catch (NotMarketMakerException | UserBlockedException failure) {
            fail(response, 403, failure.getMessage());
        } catch (EventNotFoundException | UserNotFoundException failure) {
            fail(response, 404, failure.getMessage());
        } catch (EventAlreadyStartedException | EventNotActiveException | EventNotStartedException failure) {
            fail(response, 409, failure.getMessage());
        } catch (InvalidFileContentException failure) {
            fail(response, 400, failure.getMessage() + "\n" + String.join("\n", failure.getProblems()));
        } catch (GuessMarketException | IllegalArgumentException failure) {
            fail(response, 400, failure.getMessage() == null ? "The request is not valid." : failure.getMessage());
        } catch (Exception failure) {
            getServletContext().log("An unexpected Guess Market request error occurred.", failure);
            fail(response, 500, "The server could not complete this request. Please try again.");
        }
    }

    private Object dispatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null) {
            path = "/";
        }
        if (path.equals("/health")) {
            requireMethod(request, response, "GET");
            return new MessageDto("Guess Market is ready.");
        }
        if (path.equals("/login")) {
            requireMethod(request, response, "POST");
            JsonInput input = JsonInput.read(request);
            String name = input.string("username");
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            return application.login(session, name);
        }
        HttpSession session = request.getSession(false);
        application.requireAuthenticated(session);
        return switch (path) {
            case "/snapshot" -> {
                requireMethod(request, response, "GET");
                yield application.snapshot(session);
            }
            case "/logout" -> {
                requireMethod(request, response, "POST");
                application.logout(session);
                yield new MessageDto("You have logged out.");
            }
            case "/upload" -> {
                requireMethod(request, response, "POST");
                yield upload(request, session);
            }
            case "/quote" -> {
                requireMethod(request, response, "GET");
                int eventId = integerParameter(request, "eventId");
                int optionIndex = integerParameter(request, "optionIndex");
                long shares = longParameter(request, "shares");
                yield application.authenticated(session, (engine, username) ->
                        new QuoteDto(engine.quotePurchaseCost(eventId, optionIndex, shares)));
            }
            case "/funds", "/events/open", "/events/close", "/events/buy", "/events/order", "/events/create", "/chat" -> {
                requireMethod(request, response, "POST");
                JsonInput input = JsonInput.read(request);
                input.requireSessionIdentity();
                yield command(path, input, session);
            }
            default -> throw new ApiException(404, "This endpoint does not exist.");
        };
    }

    private Object command(String path, JsonInput input, HttpSession session) {
        return application.authenticated(session, (engine, username) -> switch (path) {
            case "/funds" -> engine.depositFunds(username, input.number("amount"));
            case "/events/open" -> engine.openEvent(new OpenEventRequestDto(username, input.integer("eventId")));
            case "/events/close" -> engine.closeEvent(new CloseEventRequestDto(username,
                    input.integer("eventId"), input.integer("winningOptionIndex")));
            case "/events/buy" -> engine.purchaseShares(new PurchaseRequestDto(username,
                    input.integer("eventId"), input.integer("optionIndex"), input.longInteger("shares")));
            case "/events/order" -> engine.submitOrder(new SubmitOrderRequestDto(username,
                    input.integer("eventId"), input.integer("optionIndex"), input.enumeration("side", OrderSide.class),
                    input.longInteger("quantity"), input.number("pricePerShare")));
            case "/events/create" -> {
                TradingMethod method = input.enumeration("tradingMethod", TradingMethod.class);
                yield engine.createEvent(new CreateEventRequestDto(username, input.string("name"),
                        input.string("description"), input.integer("commissionPercent"),
                        input.enumeration("commissionType", CommissionType.class), input.strings("optionNames"), method,
                        method == TradingMethod.LMSR ? input.integer("liquidityParameter") : 0,
                        method == TradingMethod.ORDER_BOOK ? input.longInteger("initialInvestment") : 0,
                        method == TradingMethod.ORDER_BOOK ? input.integer("baseValue") : 0,
                        method == TradingMethod.ORDER_BOOK && input.bool("mintAllowed")));
            }
            case "/chat" -> application.sendChat(session, input.string("message"));
            default -> throw new ApiException(404, "This endpoint does not exist.");
        });
    }

    private LoadResultDto upload(HttpServletRequest request, HttpSession session) throws IOException {
        String type = request.getContentType();
        if (type == null || !type.split(";", 2)[0].strip().toLowerCase(Locale.ROOT).equals("multipart/form-data")) {
            throw new ApiException(415, "Choose an XML file and upload it as multipart/form-data.");
        }
        Collection<Part> parts;
        try {
            parts = request.getParts();
        } catch (IllegalStateException tooLarge) {
            throw new ApiException(413, "The XML file must be no larger than 2 MB.");
        } catch (ServletException malformed) {
            throw new ApiException(400, "The file upload is not a valid multipart request.");
        }
        if (parts.size() != 1) {
            throw new ApiException(400, "Upload exactly one XML file in the 'file' field.");
        }
        Part file = parts.iterator().next();
        String submittedName = file.getSubmittedFileName();
        if (!file.getName().equals("file") || submittedName == null || submittedName.isBlank()) {
            throw new ApiException(400, "The upload must contain an XML file in the 'file' field.");
        }
        // This is only a display label. No user-supplied path is opened on the server.
        String name = submittedName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new ApiException(400, "Choose a file with the .xml extension.");
        }
        if (file.getSize() == 0 || file.getSize() > MAX_FILE_BYTES) {
            throw new ApiException(file.getSize() == 0 ? 400 : 413, "The XML file must contain data and be no larger than 2 MB.");
        }
        byte[] content;
        try (InputStream stream = file.getInputStream()) {
            content = stream.readNBytes(MAX_FILE_BYTES + 1);
        }
        if (content.length > MAX_FILE_BYTES) {
            throw new ApiException(413, "The XML file must be no larger than 2 MB.");
        }
        String fileName = name;
        return application.authenticated(session, (engine, username) ->
                engine.uploadEvents(username, new ByteArrayInputStream(content), fileName));
    }

    private void requireMethod(HttpServletRequest request, HttpServletResponse response, String expected) {
        if (!expected.equals(request.getMethod())) {
            response.setHeader("Allow", expected);
            throw new ApiException(405, "Use " + expected + " for this endpoint.");
        }
    }

    private long longParameter(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length != 1) {
            throw new ApiException(400, "Supply one '" + name + "' parameter.");
        }
        try {
            return Long.parseLong(values[0]);
        } catch (NumberFormatException invalid) {
            throw new ApiException(400, "'" + name + "' must be a whole number.");
        }
    }

    private int integerParameter(HttpServletRequest request, String name) {
        long value = longParameter(request, name);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new ApiException(400, "'" + name + "' is outside the integer range.");
        }
        return (int) value;
    }

    private void fail(HttpServletResponse response, int status, String message) throws IOException {
        if (!response.isCommitted()) {
            response.resetBuffer();
            response.setStatus(status);
            write(response, new MessageDto(message));
        }
    }

    private void write(HttpServletResponse response, Object result) throws IOException {
        response.getWriter().write(JSON.toJson(result));
    }
}
