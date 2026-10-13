package guessmarket.tests;

import guessmarket.client.ApiClient;
import guessmarket.dto.LoadResultDto;
import guessmarket.dto.StateTransferResultDto;
import guessmarket.dto.UserDto;
import guessmarket.dto.network.ChatMessageDto;
import guessmarket.dto.network.SnapshotDto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Tests the actual packaged Java client's cookies and JSON records against Tomcat. */
public final class ApiClientIntegrationTest {
    private ApiClientIntegrationTest() { }

    public static void main(String[] arguments) throws Exception {
        ApiClient first = new ApiClient(arguments[0]);
        ApiClient second = new ApiClient(arguments[0]);
        first.login("Java Client One");
        second.login("Java Client Two");
        first.post("funds", Map.of("amount", 500), UserDto.class);
        SnapshotDto mine = first.snapshot();
        require(mine.self().name().equals("Java Client One"), "Cookie identity did not survive requests");
        require(mine.self().balance() == 500, "Deposit not visible in deserialized snapshot");
        require(second.snapshot().self().balance() == 0, "Two Java clients share a cookie store");
        require(mine.events().size() >= 4, "Shared course events did not deserialize");
        require(!mine.priceHistories().isEmpty(), "Map keys or price histories did not deserialize");
        require(!mine.accountEntries().isEmpty(), "Private ledger did not deserialize");
        try {
            mine.events().clear();
            throw new AssertionError("Snapshot list must be immutable");
        } catch (UnsupportedOperationException expected) { }
        first.post("chat", Map.of("message", "Java client round trip"), ChatMessageDto.class);
        require(second.snapshot().chat().stream().anyMatch(entry -> entry.message().equals("Java client round trip")),
                "Chat did not propagate between Java clients");
        Path stateFile = Files.createTempFile(Path.of("out"), "java-client-state-", ".gmstate");
        double savedBalance = first.snapshot().self().balance();
        try {
            first.downloadState(stateFile);
            first.post("funds", Map.of("amount", 17), UserDto.class);
            StateTransferResultDto restored = first.uploadState(stateFile);
            require(restored.userCount() >= 2, "Java state multipart response was not deserialized");
            require(first.snapshot().self().balance() == savedBalance,
                    "Java state restore did not restore the complete account");
        } finally {
            Files.deleteIfExists(stateFile);
        }
        Path file = Files.createTempFile(Path.of("out"), "java-client-upload-", ".xml");
        try {
            String xml = Files.readString(Path.of("data/ex3/small.xml"))
                    .replace("Mujtaba is Dead", "Java Client Uploaded Event");
            Files.writeString(file, xml);
            LoadResultDto upload = first.upload(file);
            require(upload.eventCount() == 1, "Java multipart body failed");
            require(second.snapshot().events().stream().anyMatch(event ->
                    event.event().name().equals("Java Client Uploaded Event")
                    && event.event().marketMakerName().equals("Java Client One")), "Java upload ownership wrong");
        } finally {
            Files.deleteIfExists(file);
        }
        first.post("logout", Map.of(), Void.class);
        try {
            first.snapshot();
            throw new AssertionError("Logged-out Java client remained authenticated");
        } catch (ApiClient.ApiException expected) {
            require(expected.status() == 401, "Unauthorized error lost HTTP status");
            require(!expected.getMessage().isBlank(), "Server error lost its explanation");
        }
        require(second.snapshot().self().name().equals("Java Client Two"), "First logout affected second client");
        second.post("logout", Map.of(), Void.class);
        System.out.println("PASS: packaged Java client cookies, JSON, multipart and account isolation");
    }

    private static void require(boolean valid, String reason) {
        if (!valid) throw new AssertionError(reason);
    }
}
