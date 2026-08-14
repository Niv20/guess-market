# Guess Market — Exercise 1

A console prediction market written in Java 25. Events are loaded from an XML file, priced
with the **Logarithmic Market Scoring Rule (LMSR)**, traded against their own event account,
and finally resolved with one winning option.

The full documentation that accompanies the submission is in [`readme.docx`](readme.docx).

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
| `engine` | `guessmarket.engine` | Domain model, LMSR pricing, XML loading and validation, the `GuessMarketEngine` interface and its implementation |
| `ui` | `guessmarket.ui` | The console menu, input handling and all printing; holds `main` |

The engine never depends on the user interface, never returns formatted text, and never hands
out its own domain objects. Everything that leaves it is a fresh, read-only DTO.

## Building and running

Java 25 or newer is required. No build tool needs to be installed — the scripts use `javac`
and `jar` directly, and the JAXB jars the engine needs are committed under `lib/`.

```bash
./build.sh
```

That compiles each module in turn and writes a runnable program to `out/artifacts/`:
`dto.jar`, `engine.jar`, `ui.jar`, a `lib/` folder and the run scripts. Then:

```bash
./run.sh
```

On Windows, run `build.bat` and then `run.bat`, or double-click `run.bat` inside the
extracted submission folder.

To build the zip that is handed in:

```bash
./package.sh
```

## Sample data

`data/` holds files to try the system with — valid ones, files with a duplicate event id, a
commission outside the allowed range, several problems at once, a file that is not well formed,
and a folder whose name contains spaces.

## Bonus

Bonus 1, saving and loading the system state, is implemented as menu commands 7 and 8.
