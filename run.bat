@echo off
REM ---------------------------------------------------------------------------
REM  Guess Market - exercise 1
REM
REM  Keep this file next to ui.jar, engine.jar, dto.jar and the lib folder.
REM  Java 25 or newer must be installed and reachable from the command line.
REM ---------------------------------------------------------------------------

cd /d "%~dp0"

java -version >nul 2>&1
if errorlevel 1 (
    echo Java was not found on this computer.
    echo Install Java 25 or newer, then run this file again.
    pause
    exit /b 1
)

java -jar ui.jar

pause
