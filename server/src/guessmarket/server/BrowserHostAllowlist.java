package guessmarket.server;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Exact Host allowlist for requests made by an approved browser origin. */
final class BrowserHostAllowlist {
    static final String DEFAULT_HOSTS = "localhost,127.0.0.1,::1";
    private static final String PROPERTY = "guessmarket.allowedHosts";
    private static final String ENVIRONMENT = "GUESS_MARKET_ALLOWED_HOSTS";

    private final Set<String> hosts;

    private BrowserHostAllowlist(Set<String> hosts) {
        this.hosts = Set.copyOf(hosts);
    }

    static BrowserHostAllowlist configured() {
        String configured = System.getProperty(PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(ENVIRONMENT);
        }
        if (configured == null || configured.isBlank()) {
            configured = DEFAULT_HOSTS;
        }
        return parse(configured);
    }

    static BrowserHostAllowlist parse(String configured) {
        Set<String> parsed = new LinkedHashSet<>();
        for (String value : configured.split(",", -1)) {
            String host = value.strip();
            String normalized = normalize(host, false);
            if (normalized == null) {
                throw new IllegalArgumentException("Invalid allowed browser host: " + host);
            }
            parsed.add(normalized);
        }
        return new BrowserHostAllowlist(parsed);
    }

    boolean allows(String requestHost) {
        if (requestHost == null) {
            return false;
        }
        String host = requestHost;
        if (host.startsWith("[") || host.endsWith("]")) {
            if (!(host.startsWith("[") && host.endsWith("]"))) {
                return false;
            }
            host = host.substring(1, host.length() - 1);
        }
        String normalized = normalize(host, true);
        return normalized != null && hosts.contains(normalized);
    }

    private static String normalize(String host, boolean requestValue) {
        if (host.isEmpty() || host.length() > 253 || host.indexOf('*') >= 0
                || host.indexOf('/') >= 0 || host.indexOf('\\') >= 0
                || host.indexOf('?') >= 0 || host.indexOf('#') >= 0 || host.indexOf('@') >= 0
                || host.codePoints().anyMatch(Character::isWhitespace)) {
            return null;
        }
        if (!requestValue && (host.startsWith("[") || host.endsWith("]"))) {
            return null;
        }
        if (host.indexOf(':') >= 0) {
            return normalizeIpv6(host);
        }
        if (host.codePoints().allMatch(character -> Character.isDigit(character) || character == '.')) {
            return normalizeIpv4(host);
        }
        return normalizeHostname(host);
    }

    private static String normalizeIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        StringBuilder normalized = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || (part.length() > 1 && part.charAt(0) == '0')
                    || !part.codePoints().allMatch(Character::isDigit)) {
                return null;
            }
            int octet;
            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException invalid) {
                return null;
            }
            if (octet > 255) {
                return null;
            }
            if (!normalized.isEmpty()) {
                normalized.append('.');
            }
            normalized.append(octet);
        }
        return normalized.toString();
    }

    private static String normalizeIpv6(String host) {
        if (host.indexOf('%') >= 0) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return address instanceof Inet6Address
                    ? address.getHostAddress().toLowerCase(Locale.ROOT) : null;
        } catch (UnknownHostException invalid) {
            return null;
        }
    }

    private static String normalizeHostname(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        for (String label : normalized.split("\\.", -1)) {
            if (label.isEmpty() || label.length() > 63
                    || !asciiLetterOrDigit(label.charAt(0))
                    || !asciiLetterOrDigit(label.charAt(label.length() - 1))) {
                return null;
            }
            for (int index = 1; index < label.length() - 1; index++) {
                char character = label.charAt(index);
                if (!asciiLetterOrDigit(character) && character != '-') {
                    return null;
                }
            }
        }
        return normalized;
    }

    private static boolean asciiLetterOrDigit(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9';
    }
}
