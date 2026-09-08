# Guess Market — Exercise 2

A prediction market written in Java 25 with a **JavaFX** interface. Events are loaded from an
XML file, are run by a **market maker**, and are traded either against the event itself with the
**Logarithmic Market Scoring Rule (LMSR)** or between participants through an **Order Book**.

The full documentation that accompanies the submission — including every assumption made — is in
[`readme.docx`](readme.docx).

<img width="1471" height="960" alt="SCR-20260908-kufc" src="https://github.com/user-attachments/assets/4eb32782-ca6e-4e28-922e-6b1cc55a3898" />
<img width="1510" height="1056" alt="SCR-20260908-kvau" src="https://github.com/user-attachments/assets/968c7b38-f017-4de5-b36d-f41978873c40" />
<img width="1471" height="1039" alt="SCR-20260908-kupx" src="https://github.com/user-attachments/assets/21464815-b9a8-4ea4-871c-b2a6fb6b4964" />
<img width="1472" height="960" alt="SCR-20260908-kxjd" src="https://github.com/user-attachments/assets/a5880833-4525-4cb5-88cb-7444115fac08" />


## What it does

- Loads a system details file through a file chooser, on a JavaFX `Task` with a progress bar.
- Manages many users, each with an account that can be spent down and, if it ever goes below
  zero, blocks its owner from acting again.
- Runs two kinds of event side by side: LMSR, and an order book with matching, partial fills and
  minting between two buyers of opposite options.
- Shows every event with three filters over it, and every user with what they hold, what they
  have done and what they may do next.
- Keeps every preference behind one settings sheet, opened by the cog in the top right corner:
  the skin, the animations, and saving or reopening the whole system.

## Modules

```
        dto  (dto.jar)
        ^          ^
       ui   ->   engine
   (ui.jar)     (engine.jar)
```

| Module | Package root | Responsibility |
|---|---|---|
| `dto` | `guessmarket.dto` | Immutable records carrying data between the layers |
| `engine` | `guessmarket.engine` | Domain model, LMSR pricing, the order book, XML loading and validation, the `GuessMarketEngine` interface and its implementation |
| `ui` | `guessmarket.ui` | The JavaFX window: layout files, stylesheets, controllers and the loading task; holds `main` |

The engine never depends on the user interface, never returns formatted text, never hands out its
own domain objects, and has no properties and no listeners on it: it is asked, and it never
announces. Everything that leaves it is a fresh, read-only DTO.

## Building and running

Java 25 or newer is required. No build tool needs to be installed — the scripts use `javac` and
`jar` directly. JavaFX is not part of the JDK, so the JavaFX 25 runtime is committed under
`lib/javafx/`, one folder per operating system (`win`, `mac-aarch64`, `linux`), and the scripts
pick the one that matches the machine.

```bash
./build.sh
```

That compiles each module in turn and writes a runnable program to `out/artifacts/`: `dto.jar`,
`engine.jar`, `ui.jar`, a `lib/` folder and the run scripts. Then:

```bash
./run.sh
```

On Windows, run `build.bat` and then `run.bat`, or double-click `run.bat` inside the extracted
submission folder. To build the zip that is handed in:

```bash
./package.sh
```

The zip carries the Windows build of JavaFX only, and unpacks into a single folder that runs
straight away.

## Sample data

`data/` holds files to try the system with. `full-market.xml` is the one to start with: twelve
events and ten users covering both trading methods, both commission methods and every edge of the
rules. `full-market-in-play.gmstate` is that same market after 86 actions — 67 trades, 14 resting
orders, two closed events, one event created inside the program and one blocked user — and is
opened through **Settings → Load state**. The other three files whose names do not begin with
`invalid` are smaller valid ones; every `invalid-` file breaks exactly one rule and is named after
it, so the message the program shows can be checked against the file. `data/folder with spaces/`
is there to show that a path containing spaces causes no trouble.

## Bonuses

All four bonuses of this exercise are implemented, and the bonus of exercise 1 still works.

| Bonus | Where |
|---|---|
| Skins — three colour schemes, each with its own background, buttons and label font | the **Skin** row of the settings, default **Midnight** |
| Animations — five, none longer than half a second, off by default | the **Animations** switch in the settings |
| Graphs — option price per transaction, and account balance per action | inside an event's details, and inside a user's details |
| Creating an event | the **Create an event** row at the foot of the events list |
| Exercise 1: saving and loading the system | the **Save state** and **Load state** rows of the settings |

The settings sheet is opened by the cog in the top right corner of the window.
