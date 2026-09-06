@echo off
REM Guess Market - requires Java 25 or newer on the PATH.
REM
REM JavaFX is not part of the JDK, so it is handed to the java command from the lib\javafx\win
REM folder that ships next to these jars. Nothing has to be installed for this to work.
cd /d "%~dp0"

java --module-path "lib\javafx\win" ^
     --add-modules javafx.controls,javafx.fxml ^
     --enable-native-access=javafx.graphics ^
     -jar ui.jar

if errorlevel 1 (
    echo.
    echo Guess Market could not be started. Check that "java -version" reports 25 or newer.
    pause
)
