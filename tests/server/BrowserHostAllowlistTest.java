package guessmarket.server;

/** Focused parsing and matching checks for the browser-facing Host boundary. */
public final class BrowserHostAllowlistTest {
    private BrowserHostAllowlistTest() { }

    public static void main(String[] arguments) {
        BrowserHostAllowlist hosts = BrowserHostAllowlist.parse(
                "localhost,127.0.0.1,::1,Play-Server.local,192.168.50.7,fd00::7");
        require(hosts.allows("LOCALHOST"), "hostname matching was not case-insensitive");
        require(hosts.allows("127.0.0.1"), "IPv4 loopback was rejected");
        require(hosts.allows("[::1]"), "bracketed request IPv6 was rejected");
        require(hosts.allows("0:0:0:0:0:0:0:1"), "equivalent IPv6 spelling did not match");
        require(hosts.allows("play-server.LOCAL"), "configured LAN hostname was rejected");
        require(hosts.allows("192.168.50.7"), "configured LAN IPv4 was rejected");
        require(hosts.allows("fd00:0:0:0:0:0:0:7"), "configured LAN IPv6 was rejected");
        require(!hosts.allows("rebind.example"), "unconfigured hostname was accepted");
        require(!hosts.allows("127.0.0.2"), "unconfigured loopback address was accepted");

        for (String invalid : new String[] {"*", "*.example", "https://server.example",
                "server.example/path", "server.example:8080", "[::1]", "127.0.0.1:8080",
                "256.1.1.1", "127.00.0.1", "bad_host", "", "one,,two"}) {
            try {
                BrowserHostAllowlist.parse(invalid);
                throw new AssertionError("invalid browser host configuration was accepted: " + invalid);
            } catch (IllegalArgumentException expected) { }
        }
        System.out.println("PASS: exact configurable browser Host allowlist");
    }

    private static void require(boolean valid, String reason) {
        if (!valid) throw new AssertionError(reason);
    }
}
