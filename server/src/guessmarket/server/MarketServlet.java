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
import guessmarket.dto.network.HealthResponseDto;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * JSON endpoints translating credential-bound requests to engine commands. Native Servlet multipart
 * stays in RAM: the threshold covers the entire permitted request and Part.write is never used.
 */
@WebServlet(name = "GuessMarketApi", urlPatterns = "/api/*", loadOnStartup = 1)
@MultipartConfig(fileSizeThreshold = 2_162_688, maxFileSize = 2_097_152, maxRequestSize = 2_162_688)
public final class MarketServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int MAX_FILE_BYTES = 2_097_152;
    private static final String DEFAULT_ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String ALLOWED_ORIGINS_PROPERTY = "guessmarket.allowedOrigins";
    private static final String ALLOWED_ORIGINS_ENV = "GUESS_MARKET_ALLOWED_ORIGINS";
    private static final Gson JSON = new GsonBuilder().create();
    private ApplicationService application;
    private Set<String> allowedOrigins;
    private BrowserHostAllowlist allowedBrowserHosts;

    @Override
    public void init() throws ServletException {
        Object service = getServletContext().getAttribute(ApplicationService.CONTEXT_ATTRIBUTE);
        if (!(service instanceof ApplicationService market)) {
            throw new ServletException("The market application has not been initialized.");
        }
        application = market;
        allowedOrigins = configuredAllowedOrigins();
        try {
            allowedBrowserHosts = BrowserHostAllowlist.configured();
        } catch (IllegalArgumentException invalid) {
            throw new ServletException(invalid.getMessage(), invalid);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Vary", "Origin");
        try {
            configureCors(request, response);
            if ("OPTIONS".equals(request.getMethod())) {
                preflight(response);
                return;
            }
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
            return new HealthResponseDto("Guess Market is ready.", "guess-market", 2);
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
        String bearerToken = bearerToken(request);
        application.requireAuthenticated(session, bearerToken);
        return switch (path) {
            case "/snapshot" -> {
                requireMethod(request, response, "GET");
                yield application.snapshot(session, bearerToken);
            }
            case "/logout" -> {
                requireMethod(request, response, "POST");
                application.logout(session, bearerToken);
                yield new MessageDto("You have logged out.");
            }
            case "/upload" -> {
                requireMethod(request, response, "POST");
                yield upload(request, session, bearerToken);
            }
            case "/quote" -> {
                requireMethod(request, response, "GET");
                int eventId = integerParameter(request, "eventId");
                int optionIndex = integerParameter(request, "optionIndex");
                long shares = longParameter(request, "shares");
                yield application.authenticated(session, bearerToken, (engine, username) ->
                        new QuoteDto(engine.quotePurchaseCost(eventId, optionIndex, shares)));
            }
            case "/funds", "/events/open", "/events/close", "/events/buy", "/events/order", "/events/create", "/chat" -> {
                requireMethod(request, response, "POST");
                JsonInput input = JsonInput.read(request);
                input.requireSessionIdentity();
                yield command(path, input, session, bearerToken);
            }
            default -> throw new ApiException(404, "This endpoint does not exist.");
        };
    }

    private Object command(String path, JsonInput input, HttpSession session, String bearerToken) {
        return application.authenticated(session, bearerToken, (engine, username) -> switch (path) {
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
            case "/chat" -> application.sendChat(session, bearerToken, input.string("message"));
            default -> throw new ApiException(404, "This endpoint does not exist.");
        });
    }

    private LoadResultDto upload(HttpServletRequest request, HttpSession session, String bearerToken) throws IOException {
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
        return application.authenticated(session, bearerToken, (engine, username) ->
                engine.uploadEvents(username, new ByteArrayInputStream(content), fileName));
    }

    private void configureCors(HttpServletRequest request, HttpServletResponse response) {
        Enumeration<String> values = request.getHeaders("Origin");
        if (!values.hasMoreElements()) {
            return;
        }
        String origin = values.nextElement();
        if (values.hasMoreElements() || !allowedOrigins.contains(origin)) {
            throw new ApiException(403, "This website is not allowed to use the local Guess Market server.");
        }
        if (!allowedBrowserHosts.allows(request.getServerName())) {
            throw new ApiException(403, "This server address is not approved for browser access.");
        }
        response.setHeader("Access-Control-Allow-Origin", origin);
    }

    private void preflight(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        // Compatibility with Chrome's older preflight-based Private Network Access rollout.
        response.setHeader("Access-Control-Allow-Private-Network", "true");
        response.setHeader("Allow", "GET, POST, OPTIONS");
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setContentLength(0);
    }

    private String bearerToken(HttpServletRequest request) {
        Enumeration<String> values = request.getHeaders("Authorization");
        if (!values.hasMoreElements()) {
            return null;
        }
        String authorization = values.nextElement();
        if (values.hasMoreElements() || authorization.length() != 50
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw invalidAuthorization();
        }
        String token = authorization.substring(7);
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            throw invalidAuthorization();
        }
        return token;
    }

    private ApiException invalidAuthorization() {
        return new ApiException(401, "Your session has ended. Please log in.");
    }

    private Set<String> configuredAllowedOrigins() throws ServletException {
        String configured = System.getProperty(ALLOWED_ORIGINS_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(ALLOWED_ORIGINS_ENV);
        }
        if (configured == null || configured.isBlank()) {
            configured = DEFAULT_ALLOWED_ORIGIN;
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : configured.split(",", -1)) {
            String origin = value.strip();
            if (!validOrigin(origin)) {
                throw new ServletException("Invalid allowed web origin: " + origin);
            }
            result.add(origin);
        }
        return Set.copyOf(result);
    }

    private boolean validOrigin(String origin) {
        try {
            URI uri = new URI(origin);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && !uri.getRawAuthority().endsWith(":") && uri.getPath().isEmpty()
                    && uri.getQuery() == null && uri.getFragment() == null;
        } catch (URISyntaxException invalid) {
            return false;
        }
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
