@echo off
setlocal enabledelayedexpansion

REM ---------------------------------------------------------------------------
REM  Builds Guess Market into one jar per module, the same way build.sh does.
REM
REM    out\classes\...   compiled classes, one folder per module
REM    out\artifacts\    dto.jar, engine.jar, ui.jar, the lib folder, run scripts
REM
REM  Java 25 or newer must be installed and reachable from the command line.
REM ---------------------------------------------------------------------------

cd /d "%~dp0"

set "CLASSES=out\classes"
set "ARTIFACTS=out\artifacts"
set "WORK=out\manifests"
set "MAIN_CLASS=guessmarket.ui.GuessMarketApplication"

echo Cleaning previous build output
if exist out rmdir /s /q out
mkdir "%CLASSES%"
mkdir "%ARTIFACTS%\lib"
mkdir "%WORK%"

REM Every third party jar, written the way it will be found next to the built jars.
set "LIB_ENTRIES="
for %%J in (lib\*.jar) do set "LIB_ENTRIES=!LIB_ENTRIES! lib/%%~nxJ"

echo Compiling the modules
echo   compiling dto
dir /s /b dto\src\*.java > "%WORK%\dto-sources.txt"
javac --release 25 -Xlint:all,-serial -d "%CLASSES%\dto" "@%WORK%\dto-sources.txt"
if errorlevel 1 goto :failed

echo   compiling engine
dir /s /b engine\src\*.java > "%WORK%\engine-sources.txt"
javac --release 25 -Xlint:all,-serial -classpath "%CLASSES%\dto;lib\*" -d "%CLASSES%\engine" "@%WORK%\engine-sources.txt"
if errorlevel 1 goto :failed

echo   compiling ui
dir /s /b ui\src\*.java > "%WORK%\ui-sources.txt"
javac --release 25 -Xlint:all,-serial -classpath "%CLASSES%\dto;%CLASSES%\engine" -d "%CLASSES%\ui" "@%WORK%\ui-sources.txt"
if errorlevel 1 goto :failed

echo Building the jars
> "%WORK%\dto.mf" echo Manifest-Version: 1.0
(
  echo Manifest-Version: 1.0
  echo Class-Path: dto.jar!LIB_ENTRIES!
) > "%WORK%\engine.mf"
(
  echo Manifest-Version: 1.0
  echo Class-Path: engine.jar dto.jar!LIB_ENTRIES!
  echo Main-Class: %MAIN_CLASS%
) > "%WORK%\ui.mf"

jar --create --file "%ARTIFACTS%\dto.jar" --manifest "%WORK%\dto.mf" -C "%CLASSES%\dto" .
if errorlevel 1 goto :failed
jar --create --file "%ARTIFACTS%\engine.jar" --manifest "%WORK%\engine.mf" -C "%CLASSES%\engine" .
if errorlevel 1 goto :failed
jar --create --file "%ARTIFACTS%\ui.jar" --manifest "%WORK%\ui.mf" -C "%CLASSES%\ui" .
if errorlevel 1 goto :failed

echo Copying the libraries and the run scripts
copy /y lib\*.jar "%ARTIFACTS%\lib\" >nul
copy /y run.bat "%ARTIFACTS%\" >nul
copy /y run.sh "%ARTIFACTS%\" >nul

echo.
echo Build finished. The runnable program is in %ARTIFACTS%.
goto :eof

:failed
echo.
echo The build failed. Read the errors above, fix them, and run this file again.
exit /b 1
