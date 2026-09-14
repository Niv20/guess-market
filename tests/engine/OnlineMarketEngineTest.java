package guessmarket.tests;

import guessmarket.dto.AccountEntryDto;
import guessmarket.dto.CloseEventRequestDto;
import guessmarket.dto.CommissionType;
import guessmarket.dto.CreateEventRequestDto;
import guessmarket.dto.OrderSide;
import guessmarket.dto.SubmitOrderRequestDto;
import guessmarket.dto.TradingMethod;
import guessmarket.dto.EventDto;
import guessmarket.dto.OpenEventRequestDto;
import guessmarket.dto.PurchaseRequestDto;
import guessmarket.engine.GuessMarketEngine;
import guessmarket.engine.GuessMarketEngineFactory;
import guessmarket.engine.OnlineMarketEngine;
import guessmarket.engine.exception.FileLoadException;
import guessmarket.engine.exception.InvalidFileContentException;
import guessmarket.engine.exception.InvalidShareQuantityException;
import guessmarket.engine.market.LmsrMechanism;
import guessmarket.engine.exception.UserAlreadyExistsException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone contract/regression checks, requiring no test framework. Run from the project root. */
public final class OnlineMarketEngineTest {
    private static int assertions;

    private OnlineMarketEngineTest() {
    }

    public static void main(String[] args) throws Exception {
        registrationAndDeposits();
        uploadedCourseSamples();
        atomicValidation();
        accountLedgerTracksOtherUsers();
        concurrentOperations();
        numericShareCapacity();
        previousExerciseStillWorks();
        System.out.println("OnlineMarketEngineTest: " + assertions + " checks passed.");
    }

    private static void registrationAndDeposits() {
        OnlineMarketEngine engine = GuessMarketEngineFactory.createOnlineEngine();
        check(engine.isSystemLoaded(), "online system starts available");
        check(engine.getAllEvents().isEmpty() && engine.getAllUsers().isEmpty(), "starts empty");
        check(engine.registerUser("  Alice  ").balance() == 0, "initial balance is zero");
        check(engine.getUser("alice").name().equals("Alice"), "trimmed canonical name");
        expect(UserAlreadyExistsException.class, () -> engine.registerUser("ALICE"));
        expect(IllegalArgumentException.class, () -> engine.registerUser("  "));
        expect(IllegalArgumentException.class, () -> engine.registerUser("bad\nname"));
        for (double invalid : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY}) {
            expect(IllegalArgumentException.class, () -> engine.depositFunds("Alice", invalid));
        }
        check(engine.getAccountHistory("alice").size() == 1, "failed deposit leaves no ledger row");
        engine.depositFunds("ALICE", 25.5);
        engine.depositFunds("Alice", 4.5);
        close(engine.getUser("Alice").balance(), 30, "deposits accumulate");
        List<AccountEntryDto> history = engine.getAccountHistory("Alice");
        check(history.size() == 3, "opening row and one row per deposit");
        close(history.get(2).amount(), 4.5, "ledger signed amount");
        close(history.get(2).balanceBefore(), 25.5, "ledger balance before");
        close(history.get(2).balanceAfter(), 30, "ledger balance after");
        check(history.get(2).sequence() == 2, "ledger sequence");
        expect(UnsupportedOperationException.class, () -> engine.saveSystemState("unused"));
        expect(UnsupportedOperationException.class, () -> engine.loadSystemState("unused"));
        expect(UnsupportedOperationException.class, () -> engine.loadSystemDetailsFile("unused"));
        OnlineMarketEngine another = GuessMarketEngineFactory.createOnlineEngine();
        check(another.getAllUsers().isEmpty(), "separate server lifetime has no persisted users");
        another.registerUser("Large deposit");
        another.depositFunds("Large deposit", Double.MAX_VALUE);
        expect(IllegalArgumentException.class,
                () -> another.depositFunds("Large deposit", Double.MAX_VALUE));
        check(Double.isFinite(another.getUser("Large deposit").balance()), "overflow rejected atomically");
    }

    private static void uploadedCourseSamples() throws IOException {
        OnlineMarketEngine engine = GuessMarketEngineFactory.createOnlineEngine();
        engine.registerUser("First MM");
        engine.registerUser("Second MM");
        try (InputStream small = Files.newInputStream(Path.of("data/ex3/small.xml"))) {
            check(engine.uploadEvents("First MM", small, "small.xml").eventCount() == 1,
                    "actual course small sample accepted");
        }
        try (InputStream multiple = Files.newInputStream(Path.of("data/ex3/multiple.xml"))) {
            check(engine.uploadEvents("Second MM", multiple, "multiple.xml").eventCount() == 3,
                    "actual course multiple sample accepted");
        }
        List<EventDto> events = engine.getAllEvents();
        check(events.size() == 4, "uploads accumulate");
        check(events.stream().map(EventDto::id).distinct().count() == 4, "server allocates unique IDs");
        check(engine.getUser("First MM").marketMakerEventIds().size() == 1, "first uploader MM");
        check(engine.getUser("Second MM").marketMakerEventIds().size() == 3, "second uploader MM");
        check(engine.getUser("First MM").balance() == 0, "upload does not open or subsidize events");
    }

    private static void atomicValidation() throws IOException {
        OnlineMarketEngine engine = GuessMarketEngineFactory.createOnlineEngine();
        engine.registerUser("Owner");
        upload(engine, xml(event("Existing", "10")));
        int before = engine.getAllEvents().size();
        String[] rejected = {
                xml(event("New first", "10") + event("existing", "10")),
                xml(event("Two", "10") + event(" TWO ", "10")),
                xml(event("Too high", "91")),
                xml(event("Too low", "-1")),
                xml(event("Bad number", "five")),
                xml(event("Old ID", "10").replace("<description>", "<id>1</id><description>")),
                xml(event("Old users", "10")).replace("</Guess-Market>", "<GM-users/></Guess-Market>"),
                xml(event("Duplicate field", "10").replace("<description>",
                        "<description>duplicate</description><description>")),
                xml(event("Missing field", "10").replace("<description>Question</description>", "")),
                xml(event("Bad liquidity", "10").replace("<b>100</b>", "<b>0</b>")),
                xml(event("Too many options", "10").replace("</GM-options>",
                        "<GM-option>Maybe</GM-option></GM-options>")),
                "<Guess-Market><GM-events>",
                "<!DOCTYPE Guess-Market [<!ENTITY payload SYSTEM 'file:///etc/passwd'>]>"
                        + xml(event("External", "10")).replace("Question", "&payload;"),
                "<!DOCTYPE Guess-Market [<!ENTITY payload 'internal entity'>]>"
                        + xml(event("Entity", "10")).replace("Question", "&payload;")
        };
        for (String invalid : rejected) {
            expect(FileLoadException.class, () -> upload(engine, invalid));
            check(engine.getAllEvents().size() == before, "rejected upload is atomic");
            check(engine.getUser("Owner").marketMakerEventIds().size() == before,
                    "rejected upload does not change market-maker ownership");
        }
        expect(FileLoadException.class, () -> engine.uploadEvents("Owner", stream(xml(event("Name", "10"))), "file.txt"));
        String ex2 = Files.readString(Path.of("data/single-event.xml"));
        expect(InvalidFileContentException.class, () -> upload(engine, ex2));
        upload(engine, xml(event("Valid later", "90")));
        check(engine.getAllEvents().size() == 2, "engine continues after rejected uploads");
        check(engine.getAllEvents().get(1).id() == 2, "rejected upload does not consume IDs");
    }

    private static void accountLedgerTracksOtherUsers() {
        OnlineMarketEngine engine = GuessMarketEngineFactory.createOnlineEngine();
        engine.registerUser("Owner");
        engine.registerUser("Buyer");
        engine.depositFunds("Owner", 1000);
        engine.depositFunds("Buyer", 1000);
        upload(engine, xml(event("Ledger event", "10")));
        int eventId = engine.getAllEvents().getFirst().id();
        engine.openEvent(new OpenEventRequestDto("Owner", eventId));
        int before = engine.getAccountHistory("Owner").size();
        engine.purchaseShares(new PurchaseRequestDto("Buyer", eventId, 0, 10));
        List<AccountEntryDto> ownerRows = engine.getAccountHistory("Owner");
        check(ownerRows.size() == before + 1, "another user's purchase records commission for MM");
        check(ownerRows.getLast().amount() > 0, "commission is credited");
        check(ownerRows.getLast().description().contains("Commission"), "commission operation description");
        check(engine.getAccountHistory("Buyer").getLast().amount() < 0, "purchase is a debit");
        engine.closeEvent(new CloseEventRequestDto("Owner", eventId, 0));
        check(engine.getAccountHistory("Buyer").getLast().description().contains("Won"),
                "settlement records winner payment");
        for (String user : List.of("Owner", "Buyer")) {
            List<AccountEntryDto> rows = engine.getAccountHistory(user);
            close(rows.stream().mapToDouble(AccountEntryDto::amount).sum(), engine.getUser(user).balance(),
                    "statement reconciles to balance");
            for (int i = 1; i < rows.size(); i++) {
                close(rows.get(i - 1).balanceAfter(), rows.get(i).balanceBefore(), "continuous statement");
            }
        }
    }

    private static void concurrentOperations() throws InterruptedException {
        OnlineMarketEngine engine = GuessMarketEngineFactory.createOnlineEngine();
        int threads = 12;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger registered = new AtomicInteger();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                await(start);
                try {
                    engine.registerUser("Contended");
                    registered.incrementAndGet();
                } catch (UserAlreadyExistsException expected) {
                    // The other registrations must lose, without replacing the winner.
                }
            });
            workers.add(worker);
            worker.start();
        }
        start.countDown();
        for (Thread worker : workers) worker.join();
        check(registered.get() == 1 && engine.getAllUsers().size() == 1, "atomic duplicate registration");
        workers.clear();
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                for (int deposit = 0; deposit < 100; deposit++) engine.depositFunds("Contended", 1);
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) worker.join();
        close(engine.getUser("Contended").balance(), 1200, "concurrent deposits never lose updates");
        check(engine.getAccountHistory("Contended").size() == 1201, "all concurrent deposits have rows");
        workers.clear();
        AtomicInteger uploads = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                try {
                    engine.uploadEvents("Contended", stream(xml(event("Concurrent event", "10"))), "events.xml");
                    uploads.incrementAndGet();
                } catch (InvalidFileContentException expected) {
                    // Every competing copy of the same event must be rejected.
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) worker.join();
        check(uploads.get() == 1 && engine.getAllEvents().size() == 1, "atomic duplicate upload");
        check(engine.getUser("Contended").marketMakerEventIds().size() == 1,
                "concurrent uploads do not duplicate ownership");
    }

    private static void numericShareCapacity() {
        LmsrMechanism pricing = new LmsrMechanism(100);
        close(pricing.purchaseCost(new long[]{Long.MAX_VALUE - 1, 0}, 0, 1), 1,
                "one share remains correctly priced in a very large pool");
        double balancedOneShare = 100 * Math.log1p(Math.expm1(0.01) / 2);
        close(pricing.purchaseCost(new long[]{Long.MAX_VALUE - 1, Long.MAX_VALUE - 1}, 0, 1),
                balancedOneShare, "balanced large pool preserves small price increments");
        expect(InvalidShareQuantityException.class,
                () -> pricing.purchaseCost(new long[]{Long.MAX_VALUE, 0}, 0, 1));
        double maximumError = 0;
        for (long left : new long[]{0, 1, 50, 1000}) {
            for (long right : new long[]{0, 1, 50, 1000}) {
                for (long quantity : new long[]{1, 3, 50, 2000}) {
                    double before = 100 * Math.log(Math.exp(left / 100.0) + Math.exp(right / 100.0));
                    double after = 100 * Math.log(Math.exp((left + quantity) / 100.0) + Math.exp(right / 100.0));
                    maximumError = Math.max(maximumError,
                            Math.abs(pricing.purchaseCost(new long[]{left, right}, 0, quantity) - (after - before)));
                }
            }
        }
        check(maximumError < 0.000000001, "stable formula matches ordinary LMSR prices");

        OnlineMarketEngine engine = GuessMarketEngineFactory.createOnlineEngine();
        for (String user : List.of("Owner", "Buyer", "Other", "Resale buyer")) {
            engine.registerUser(user);
            engine.depositFunds(user, 1e20);
        }
        upload(engine, xml(event("Capacity LMSR", "0")));
        int id = engine.getAllEvents().getFirst().id();
        engine.openEvent(new OpenEventRequestDto("Owner", id));
        engine.purchaseShares(new PurchaseRequestDto("Buyer", id, 0, Long.MAX_VALUE - 1));
        close(engine.quotePurchaseCost(id, 0, 1), 1, "server quotes final available share");
        engine.purchaseShares(new PurchaseRequestDto("Buyer", id, 0, 1));
        var stateBefore = engine.getEventTradingStatus(id);
        var ledgerBefore = engine.getAccountHistory("Buyer");
        expect(InvalidShareQuantityException.class, () -> engine.quotePurchaseCost(id, 0, 1));
        expect(InvalidShareQuantityException.class,
                () -> engine.purchaseShares(new PurchaseRequestDto("Buyer", id, 0, 1)));
        check(engine.getEventTradingStatus(id).equals(stateBefore), "rejected overflow leaves event unchanged");
        check(engine.getAccountHistory("Buyer").equals(ledgerBefore), "rejected overflow leaves account unchanged");
        check(stateBefore.totalSharesByOption().getFirst() == Long.MAX_VALUE,
                "LMSR holdings never wrap to a negative number");

        int bookId = engine.createEvent(new CreateEventRequestDto("Owner", "Capacity book", "Capacity test", 0,
                CommissionType.ON_PURCHASE, List.of("Yes", "No"), TradingMethod.ORDER_BOOK,
                0, 0, 1, true)).id();
        engine.openEvent(new OpenEventRequestDto("Owner", bookId));
        engine.submitOrder(new SubmitOrderRequestDto("Buyer", bookId, 0, OrderSide.BUY, Long.MAX_VALUE - 1, 0.5));
        var initialMint = engine.submitOrder(new SubmitOrderRequestDto("Other", bookId, 1,
                OrderSide.BUY, Long.MAX_VALUE, 0.5));
        check(initialMint.quantityMinted() == Long.MAX_VALUE - 1 && initialMint.quantityResting() == 1,
                "large initial mint fills only the matching quantity");
        var lastMint = engine.submitOrder(new SubmitOrderRequestDto("Buyer", bookId, 0, OrderSide.BUY, 5, 0.5));
        check(lastMint.quantityMinted() == 1 && lastMint.quantityResting() == 4,
                "mint stops at representable capacity and rests the remainder");
        var noCapacity = engine.submitOrder(new SubmitOrderRequestDto("Other", bookId, 1,
                OrderSide.BUY, Long.MAX_VALUE, 0.5));
        check(noCapacity.quantityFilled() == 0 && noCapacity.quantityResting() == Long.MAX_VALUE,
                "full market leaves further bids unfilled");
        engine.submitOrder(new SubmitOrderRequestDto("Buyer", bookId, 0, OrderSide.BUY, Long.MAX_VALUE, 0.5));
        check(engine.getEventTradingStatus(bookId).totalSharesByOption()
                .equals(List.of(Long.MAX_VALUE, Long.MAX_VALUE)),
                "multiple enormous resting orders do not overflow minted supply");
        engine.submitOrder(new SubmitOrderRequestDto("Resale buyer", bookId, 0, OrderSide.BUY, 1, 0.6));
        var resale = engine.submitOrder(new SubmitOrderRequestDto("Buyer", bookId, 0, OrderSide.SELL, 1, 0.6));
        check(resale.quantityFilled() == 1 && resale.quantityMinted() == 0,
                "existing shares can still be resold at full issuance capacity");
        check(engine.getEventTradingStatus(bookId).totalSharesByOption()
                .equals(List.of(Long.MAX_VALUE, Long.MAX_VALUE)), "resale preserves bounded total supply");
    }

    private static void previousExerciseStillWorks() {
        GuessMarketEngine engine = GuessMarketEngineFactory.createEngine();
        check(!engine.isSystemLoaded(), "Exercise 2 still starts unloaded");
        engine.loadSystemDetailsFile("data/single-event.xml");
        check(engine.isSystemLoaded() && engine.getAllUsers().size() > 0, "Exercise 2 file loading retained");
        engine.loadSystemDetailsFile("data/multiple-events.xml");
        check(engine.getAllEvents().size() > 1, "Exercise 2 replacement loading retained");
    }

    private static void upload(OnlineMarketEngine engine, String xml) {
        engine.uploadEvents("Owner", stream(xml), "events.xml");
    }

    private static ByteArrayInputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String xml(String events) {
        return "<Guess-Market><GM-events>" + events + "</GM-events></Guess-Market>";
    }

    private static String event(String name, String commission) {
        return "<GM-event name=\"" + name + "\"><description>Question</description>"
                + "<commission type=\"on-purchase\">" + commission + "</commission>"
                + "<GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>"
                + "<GM-method><GM-LMSR><b>100</b></GM-LMSR></GM-method></GM-event>";
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.000001, message + ": " + actual + " != " + expected);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Expected " + type.getSimpleName() + ", got " + error, error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
