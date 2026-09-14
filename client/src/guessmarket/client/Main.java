package guessmarket.client;

import javafx.application.Application;

/** Non-Application launcher works with JavaFX on the module path and application jars on the classpath. */
public final class Main {
    private Main() { }
    public static void main(String[] args) { Application.launch(GuessMarketClient.class, args); }
}
