@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "CLASSES=out\classes"
set "ARTIFACTS=out\artifacts"
set "WORK=out\manifests"
set "JAVAFX=lib\javafx\win"
for %%D in ("%CLASSES%" "%ARTIFACTS%" "%WORK%" "out\war") do if exist %%D rmdir /s /q %%D
mkdir "%CLASSES%" "%ARTIFACTS%\client\lib" "%WORK%" "out\war\WEB-INF\lib"
call :compile dto "" ""
if errorlevel 1 goto :failed
call :compile engine "%CLASSES%\dto;lib\*" ""
if errorlevel 1 goto :failed
call :resources engine
call :compile ui "%CLASSES%\dto;%CLASSES%\engine" javafx
if errorlevel 1 goto :failed
call :resources ui
mkdir "%CLASSES%\ui-common\guessmarket\ui"
xcopy /e /i /q /y "%CLASSES%\ui\guessmarket\ui\common" "%CLASSES%\ui-common\guessmarket\ui\common" >nul
xcopy /e /i /q /y "%CLASSES%\ui\guessmarket\ui\css" "%CLASSES%\ui-common\guessmarket\ui\css" >nul
mkdir "%CLASSES%\ui-common\guessmarket\ui\events" "%CLASSES%\ui-common\guessmarket\ui\users"
for %%V in (EventDetailsController OptionBookController EventTile) do copy /y "%CLASSES%\ui\guessmarket\ui\events\%%V*.class" "%CLASSES%\ui-common\guessmarket\ui\events\" >nul
copy /y "%CLASSES%\ui\guessmarket\ui\users\UserInvolvementController*.class" "%CLASSES%\ui-common\guessmarket\ui\users\" >nul
for %%R in (event-details option-book) do copy /y "%CLASSES%\ui\guessmarket\ui\events\%%R.fxml" "%CLASSES%\ui-common\guessmarket\ui\events\" >nul
copy /y "%CLASSES%\ui\guessmarket\ui\users\user-involvement.fxml" "%CLASSES%\ui-common\guessmarket\ui\users\" >nul
call :compile server "%CLASSES%\dto;%CLASSES%\engine;lib\*;lib\provided\*" ""
if errorlevel 1 goto :failed
call :compile client "%CLASSES%\dto;%CLASSES%\ui-common;lib\gson-2.13.2.jar;lib\provided\*" javafx
if errorlevel 1 goto :failed
call :resources client
set "LIB_ENTRIES="
for %%J in (lib\*.jar) do set "LIB_ENTRIES=!LIB_ENTRIES! lib/%%~nxJ"
call :manifest dto "" ""
call :manifest engine "dto.jar!LIB_ENTRIES!" ""
call :manifest ui "engine.jar dto.jar!LIB_ENTRIES!" guessmarket.ui.Main
call :manifest ui-common "dto.jar" ""
call :manifest client "dto.jar ui-common.jar lib/gson-2.13.2.jar" guessmarket.client.Main
for %%M in (dto engine ui ui-common client) do (
    jar --create --file "%ARTIFACTS%\%%M.jar" --manifest "%WORK%\%%M.mf" -C "%CLASSES%\%%M" .
    if errorlevel 1 goto :failed
)
copy /y "%ARTIFACTS%\engine.jar" "out\war\WEB-INF\lib\" >nul
copy /y "%ARTIFACTS%\dto.jar" "out\war\WEB-INF\lib\" >nul
copy /y lib\*.jar "out\war\WEB-INF\lib\" >nul
xcopy /e /i /q /y "%CLASSES%\server" "out\war\WEB-INF\classes" >nul
xcopy /e /i /q /y server\web out\war >nul
jar --create --file "%ARTIFACTS%\guess-market.war" -C out\war .
if errorlevel 1 goto :failed
for %%M in (client dto ui-common) do copy /y "%ARTIFACTS%\%%M.jar" "%ARTIFACTS%\client\" >nul
copy /y lib\gson-2.13.2.jar "%ARTIFACTS%\client\lib\" >nul
xcopy /e /i /q /y lib\javafx "%ARTIFACTS%\client\lib\javafx" >nul
copy /y run.bat "%ARTIFACTS%\client\" >nul
copy /y run.sh "%ARTIFACTS%\client\" >nul
mkdir "%ARTIFACTS%\lib"
copy /y lib\*.jar "%ARTIFACTS%\lib\" >nul
echo Build complete: %ARTIFACTS%\guess-market.war and %ARTIFACTS%\client
exit /b 0

:compile
set "MODULE=%~1"
set "FX_ARGS="
if "%~3"=="javafx" set "FX_ARGS=--module-path %JAVAFX% --add-modules javafx.controls,javafx.fxml"
mkdir "%CLASSES%\%MODULE%"
if exist "%WORK%\%MODULE%-sources.txt" del /q "%WORK%\%MODULE%-sources.txt"
for /r "%MODULE%\src" %%F in (*.java) do (
    set "SOURCE=%%F"
    set "SOURCE=!SOURCE:\=\\!"
    >> "%WORK%\%MODULE%-sources.txt" echo "!SOURCE!"
)
echo Compiling %MODULE%
javac --release 25 -Xlint:all,-serial -Werror -classpath "%~2" %FX_ARGS% -d "%CLASSES%\%MODULE%" "@%WORK%\%MODULE%-sources.txt"
exit /b %errorlevel%

:resources
set "MODULE=%~1"
for /r "%MODULE%\src" %%F in (*) do if not "%%~xF"==".java" (
    set "FULL=%%F"
    set "REL=!FULL:*\%MODULE%\src\=!"
    for %%D in ("%CLASSES%\%MODULE%\!REL!") do if not exist "%%~dpD" mkdir "%%~dpD"
    copy /y "%%F" "%CLASSES%\%MODULE%\!REL!" >nul
)
exit /b 0

:manifest
> "%WORK%\%~1.mf" echo Manifest-Version: 1.0
if not "%~2"=="" >> "%WORK%\%~1.mf" echo Class-Path: %~2
if not "%~3"=="" >> "%WORK%\%~1.mf" echo Main-Class: %~3
>> "%WORK%\%~1.mf" echo.
exit /b 0

:failed
echo Build failed. Correct the error above and run build.bat again.
exit /b 1
