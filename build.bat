@echo off
setlocal enabledelayedexpansion

REM ---------------------------------------------------------------------------
REM  Builds Guess Market into one jar per module, the same way build.sh does.
REM
REM    out\classes\...   compiled classes, one folder per module
REM    out\artifacts\    dto.jar, engine.jar, ui.jar, the lib folder, run scripts
REM
REM  Java 25 or newer must be installed and reachable from the command line.
REM  JavaFX is not part of the JDK, so it travels with the project in lib\javafx and
REM  nothing has to be installed for it.
REM ---------------------------------------------------------------------------

cd /d "%~dp0"

set "CLASSES=out\classes"
set "ARTIFACTS=out\artifacts"
set "WORK=out\manifests"
set "MAIN_CLASS=guessmarket.ui.Main"
set "JAVAFX=lib\javafx\win"
set "JAVAFX_MODULES=javafx.controls,javafx.fxml"

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
call :collect_sources dto
javac --release 25 -Xlint:all,-serial -d "%CLASSES%\dto" "@%WORK%\dto-sources.txt"
if errorlevel 1 goto :failed

echo   compiling engine
call :collect_sources engine
javac --release 25 -Xlint:all,-serial -classpath "%CLASSES%\dto;lib\*" -d "%CLASSES%\engine" "@%WORK%\engine-sources.txt"
if errorlevel 1 goto :failed

echo   compiling ui
call :collect_sources ui
javac --release 25 -Xlint:all,-serial -classpath "%CLASSES%\dto;%CLASSES%\engine" ^
      --module-path "%JAVAFX%" --add-modules %JAVAFX_MODULES% ^
      -d "%CLASSES%\ui" "@%WORK%\ui-sources.txt"
if errorlevel 1 goto :failed

REM The layout files and the stylesheets sit next to the classes that ask for them, so they are
REM copied into the same folder structure and end up inside the jar at the expected path.
echo   copying ui resources
for /r "ui\src" %%F in (*.fxml *.css) do (
  set "FULL=%%F"
  set "REL=!FULL:*\ui\src\=!"
  for %%D in ("%CLASSES%\ui\!REL!") do if not exist "%%~dpD" mkdir "%%~dpD"
  copy /y "%%F" "%CLASSES%\ui\!REL!" >nul
)

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
xcopy /e /i /q /y lib\javafx "%ARTIFACTS%\lib\javafx" >nul
copy /y run.bat "%ARTIFACTS%\" >nul
copy /y run.sh "%ARTIFACTS%\" >nul

echo.
echo Build finished. The runnable program is in %ARTIFACTS%.
goto :eof

:failed
echo.
echo The build failed. Read the errors above, fix them, and run this file again.
exit /b 1

REM ---------------------------------------------------------------------------
REM  Writes the source file names of one module to the file javac reads with @.
REM
REM  Every name is quoted and every backslash inside the quotes is doubled, and both are needed:
REM  without the quotes javac splits a name at its spaces, and inside quotes javac reads a single
REM  backslash as an escape character and swallows it. Quoting the whole name and doubling its
REM  separators is what lets the project be built from a folder such as
REM  "C:\Users\Some Name\guess-market".
REM ---------------------------------------------------------------------------
:collect_sources
set "MODULE=%~1"
if exist "%WORK%\%MODULE%-sources.txt" del /q "%WORK%\%MODULE%-sources.txt"
for /r "%MODULE%\src" %%F in (*.java) do (
  set "SOURCE=%%F"
  set "SOURCE=!SOURCE:\=\\!"
  >> "%WORK%\%MODULE%-sources.txt" echo "!SOURCE!"
)
goto :eof
