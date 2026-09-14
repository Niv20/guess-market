package guessmarket.server;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/** In-memory bearer credentials for hosted browsers; tokens disappear with the local server. */
final class AccessTokenStore {
    static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random;
    private final LongSupplier nanoClock;
    private final long idleTimeoutNanos;
    private final Map<String, TokenEntry> entries = new HashMap<>();

    AccessTokenStore() {
        this(new SecureRandom(), System::nanoTime, DEFAULT_IDLE_TIMEOUT);
    }

    AccessTokenStore(SecureRandom random, LongSupplier nanoClock, Duration idleTimeout) {
        this.random = Objects.requireNonNull(random);
        this.nanoClock = Objects.requireNonNull(nanoClock);
        Objects.requireNonNull(idleTimeout);
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("The token inactivity timeout must be positive.");
        }
        idleTimeoutNanos = idleTimeout.toNanos();
    }

    synchronized String issue(String username) {
        Objects.requireNonNull(username);
        long now = nanoClock.getAsLong();
        removeExpired(now);
        byte[] bytes = new byte[TOKEN_BYTES];
        String token;
        do {
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (entries.containsKey(token));
        entries.put(token, new TokenEntry(username, now));
        return token;
    }

    synchronized String authenticate(String token) {
        if (token == null) {
            return null;
        }
        TokenEntry entry = entries.get(token);
        if (entry == null) {
            return null;
        }
        long now = nanoClock.getAsLong();
        if (expired(entry, now)) {
            entries.remove(token);
            return null;
        }
        entries.put(token, new TokenEntry(entry.username(), now));
        return entry.username();
    }

    synchronized void revoke(String token) {
        if (token != null) {
            entries.remove(token);
        }
    }

    private void removeExpired(long now) {
        Iterator<TokenEntry> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            if (expired(iterator.next(), now)) {
                iterator.remove();
            }
        }
    }

    private boolean expired(TokenEntry entry, long now) {
        return now - entry.lastAccessNanos() >= idleTimeoutNanos;
    }

    private record TokenEntry(String username, long lastAccessNanos) {
    }
}
