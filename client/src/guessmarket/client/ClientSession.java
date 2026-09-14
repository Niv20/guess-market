package guessmarket.client;

import guessmarket.dto.network.SnapshotDto;
import javafx.application.Platform;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Serializes requests on one worker, including a one-second pull. A slow request can never overlap
 * another pull or reorder a write with a snapshot. A session generation rejects callbacks left
 * behind by logout, expiry, or a new login. Every listener callback runs on the JavaFX thread.
 */
public final class ClientSession implements AutoCloseable {
    public interface Listener {
        void loggedIn(String username);
        void loggedOut(String reason);
        void snapshotReceived(SnapshotDto snapshot);
        void busyChanged(boolean busy);
        void connectionChanged(boolean connected, String message);
        void error(String message);
    }
    @FunctionalInterface public interface Request<T> { T run(ApiClient api) throws Exception; }

    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(action -> {
        Thread thread = new Thread(action, "guess-market-http");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong generation = new AtomicLong();
    private final Listener listener;
    private volatile ApiClient api;
    private volatile ScheduledFuture<?> pull;
    private volatile boolean active;
    private boolean busy;

    public ClientSession(Listener listener) { this.listener = listener; }

    public void login(String server, String username) {
        if (busy) return;
        final ApiClient next;
        try { next = new ApiClient(server); }
        catch (IllegalArgumentException error) { listener.error(error.getMessage()); return; }
        long token = generation.incrementAndGet();
        busy = true;
        listener.busyChanged(true);
        worker.execute(() -> {
            try {
                String name = next.login(username).username();
                if (generation.get() != token) return;
                api = next;
                active = true;
                dispatch(token, () -> listener.loggedIn(name));
                pull = worker.scheduleWithFixedDelay(() -> refresh(token), 0, 1, TimeUnit.SECONDS);
            } catch (Exception error) {
                dispatch(token, () -> listener.error(explain(error)));
            } finally { dispatch(token, () -> setBusy(false)); }
        });
    }

    public <T> void perform(Request<T> request, Consumer<T> onSuccess) {
        if (!active || busy) return;
        busy = true;
        listener.busyChanged(true);
        long token = generation.get();
        worker.execute(() -> {
            try {
                if (!active || generation.get() != token) return;
                T result = request.run(api);
                dispatch(token, () -> onSuccess.accept(result));
                refresh(token);
            } catch (Exception error) {
                if (!expireIfUnauthorized(token, error)) {
                    dispatch(token, () -> listener.error(explain(error)));
                    // Never repeat a mutation after an uncertain network response. Only read state.
                    refresh(token);
                }
            } finally { dispatch(token, () -> setBusy(false)); }
        });
    }

    public void logout() {
        if (busy || !active) return;
        long token = generation.incrementAndGet();
        active = false;
        if (pull != null) pull.cancel(false);
        setBusy(true);
        worker.execute(() -> {
            String reason = "You have signed out. Your username remains reserved until the server restarts.";
            try { api.post("logout", Map.of(), Void.class); }
            catch (Exception error) { reason += " The server could not confirm sign out; this client's session has been cleared."; }
            finally { api.clearSession(); }
            String message = reason;
            dispatch(token, () -> { setBusy(false); listener.loggedOut(message); });
        });
    }

    private void refresh(long token) {
        if (!active || generation.get() != token) return;
        try {
            SnapshotDto state = api.snapshot();
            dispatch(token, () -> {
                listener.snapshotReceived(state);
                listener.connectionChanged(true, "Live · refreshes every second");
            });
        } catch (Exception error) {
            if (!expireIfUnauthorized(token, error)) {
                dispatch(token, () -> listener.connectionChanged(false,
                        "Connection interrupted · retrying automatically. " + explain(error)));
            }
        }
    }

    private boolean expireIfUnauthorized(long token, Exception error) {
        if (!(error instanceof ApiClient.ApiException apiError) || apiError.status() != 401) return false;
        if (generation.get() != token) return true;
        active = false;
        if (pull != null) pull.cancel(false);
        api.clearSession();
        long expired = generation.incrementAndGet();
        dispatch(expired, () -> {
            setBusy(false);
            listener.loggedOut("Your session ended. Log in with an available name. Names remain reserved until the server restarts.");
        });
        return true;
    }

    private void dispatch(long token, Runnable callback) {
        Platform.runLater(() -> { if (generation.get() == token) callback.run(); });
    }

    private void setBusy(boolean value) { busy = value; listener.busyChanged(value); }

    private static String explain(Exception error) {
        if (error instanceof ConnectException) return "Cannot reach the server. Start Tomcat and check the server address.";
        if (error instanceof HttpTimeoutException) return "The server took too long to respond. Check the latest account and event state before retrying an action.";
        if (error instanceof InterruptedException) return "The request was interrupted.";
        if (error instanceof IOException && !(error instanceof ApiClient.ApiException)) {
            return (error.getMessage() == null ? "The connection failed." : error.getMessage())
                    + " Check the latest state before retrying an action.";
        }
        return error.getMessage() == null ? "The operation could not be completed." : error.getMessage();
    }

    @Override public void close() {
        active = false;
        generation.incrementAndGet();
        if (pull != null) pull.cancel(true);
        worker.shutdownNow();
        if (api != null) api.clearSession();
    }
}
