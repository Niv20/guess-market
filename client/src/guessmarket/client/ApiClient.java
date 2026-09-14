package guessmarket.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.network.LoginResponseDto;
import guessmarket.dto.network.SnapshotDto;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/** HTTP transport only. Its caller owns the worker thread; this class knows nothing about JavaFX. */
public final class ApiClient {
    public static final String DEFAULT_SERVER = "http://localhost:8080/guess-market/api/";
    private static final long MAX_UPLOAD_BYTES = 2 * 1024 * 1024;
    private final Gson gson = new Gson();
    private final URI base;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    private final HttpClient http;

    public ApiClient(String server) {
        String normalized = server.trim();
        if (!normalized.endsWith("/")) normalized += "/";
        base = URI.create(normalized);
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getQuery() != null || base.getFragment() != null
                || base.getUserInfo() != null) {
            throw new IllegalArgumentException("Enter a server address such as " + DEFAULT_SERVER);
        }
        http = HttpClient.newBuilder().cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(8)).build();
    }

    public LoginResponseDto login(String username) throws IOException, InterruptedException {
        return post("login", Map.of("username", username), LoginResponseDto.class);
    }

    public SnapshotDto snapshot() throws IOException, InterruptedException {
        return get("snapshot", SnapshotDto.class);
    }

    public <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        return send(request(path).GET().build(), type);
    }

    public <T> T post(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        return send(request(path).header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build(), type);
    }

    /** Standard multipart upload, entirely in memory, without a third party upload library. */
    public LoadResultDto upload(Path file) throws IOException, InterruptedException {
        if (file.getFileName() == null || !file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".xml")) {
            throw new IOException("Choose an XML file with the .xml extension.");
        }
        if (Files.size(file) > MAX_UPLOAD_BYTES) {
            throw new IOException("The XML file must be no larger than 2 MB.");
        }
        byte[] contents;
        try (var input = Files.newInputStream(file)) {
            contents = input.readNBytes((int) MAX_UPLOAD_BYTES + 1);
        }
        if (contents.length > MAX_UPLOAD_BYTES) throw new IOException("The XML file must be no larger than 2 MB.");
        String boundary = "GuessMarket" + UUID.randomUUID().toString().replace("-", "");
        // A fixed safe multipart filename avoids CR/LF or quote injection from a local filename.
        byte[] head = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"events.xml\"\r\n"
                + "Content-Type: application/xml\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        return send(request("upload").header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(java.util.List.of(head, contents, tail))).build(),
                LoadResultDto.class);
    }

    public void clearSession() {
        cookies.getCookieStore().removeAll();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
    }

    private <T> T send(HttpRequest request, Class<T> type) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = "The server returned HTTP " + response.statusCode() + ".";
            try {
                JsonObject error = gson.fromJson(response.body(), JsonObject.class);
                if (error != null && error.has("message")) message = error.get("message").getAsString();
            } catch (RuntimeException ignored) {
                if (response.statusCode() == 404) message = "Guess Market was not found at this address. Check that guess-market.war is deployed.";
            }
            throw new ApiException(response.statusCode(), message);
        }
        if (type == Void.class || response.body().isBlank()) return null;
        try {
            T result = gson.fromJson(response.body(), type);
            if (result == null) throw new JsonParseException("Empty JSON response");
            return result;
        } catch (RuntimeException error) {
            throw new IOException("The server returned an unreadable response. Check the client and server versions.", error);
        }
    }

    public static final class ApiException extends IOException {
        private final int status;
        public ApiException(int status, String message) { super(message); this.status = status; }
        public int status() { return status; }
    }
}
