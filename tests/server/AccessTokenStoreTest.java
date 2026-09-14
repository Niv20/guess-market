package guessmarket.server;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

/** Focused checks for bearer-token entropy, inactivity refresh, expiry and revocation. */
public final class AccessTokenStoreTest {
    private AccessTokenStoreTest() { }

    public static void main(String[] arguments) {
        AtomicLong now = new AtomicLong();
        Duration timeout = Duration.ofMinutes(30);
        AccessTokenStore store = new AccessTokenStore(new SecureRandom(), now::get, timeout);

        String first = store.issue("Browser One");
        String second = store.issue("Browser Two");
        require(!first.equals(second), "independent logins reused a bearer token");
        require(first.matches("[A-Za-z0-9_-]{43}"), "token is not unpadded base64url");
        require(Base64.getUrlDecoder().decode(first).length == 32, "token is not 256 bits");
        require("Browser One".equals(store.authenticate(first)), "fresh token was rejected");

        now.addAndGet(timeout.toNanos() - 1);
        require("Browser One".equals(store.authenticate(first)), "active token expired too early");
        now.addAndGet(timeout.toNanos() - 1);
        require("Browser One".equals(store.authenticate(first)), "authentication did not refresh inactivity");
        now.addAndGet(timeout.toNanos());
        require(store.authenticate(first) == null, "inactive token survived its timeout");

        require(store.authenticate(second) == null, "another inactive token survived its timeout");
        String revocable = store.issue("Browser Three");
        require("Browser Three".equals(store.authenticate(revocable)), "new token was not usable");
        store.revoke(revocable);
        require(store.authenticate(revocable) == null, "logout-style revocation did not remove the token");
        System.out.println("PASS: 256-bit bearer token inactivity and revocation");
    }

    private static void require(boolean valid, String reason) {
        if (!valid) throw new AssertionError(reason);
    }
}
