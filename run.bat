@echo off
setlocal
REM Starts the Exercise 3 client using Java 25 and the bundled JavaFX runtime.
cd /d "%~dp0"
if not exist client.jar cd out\artifacts\client
java --module-path "lib\javafx\win" ^
    --add-modules javafx.controls,javafx.fxml ^
    --enable-native-access=javafx.graphics -jar client.jar %*
if errorlevel 1 (
    echo Guess Market could not start. Check that java -version reports 25 or newer.
    pause
    exit /b 1
)
