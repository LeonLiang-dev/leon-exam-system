@echo off
setlocal EnableExtensions

rem ============================================================
rem Leon Exam System Build Script (Windows)
rem Usage:
rem   build.bat               - Build executable JAR
rem   build.bat preflight     - Check Windows packaging prerequisites
rem   build.bat package       - Build Windows MSI installer with jpackage
rem   build.bat package-exe   - Build Windows EXE installer with jpackage
rem   build.bat package-debug - Build Windows MSI installer with console diagnostics
rem ============================================================

set "PROJECT_DIR=%~dp0"
set "WEB_DIR=%PROJECT_DIR%wts-web"
set "SERVER_DIR=%PROJECT_DIR%wts-server"
set "LAUNCHER_DIR=%PROJECT_DIR%launcher"
set "MARIADB_SOURCE_DIR=%PROJECT_DIR%packaging\windows\mariadb"
set "STATIC_DIR=%SERVER_DIR%\wts-app\src\main\resources\static"
set "JAR_NAME=wts-app-2.0.0-SNAPSHOT.jar"
set "JAR_PATH=%SERVER_DIR%\wts-app\target\%JAR_NAME%"
set "LAUNCHER_JAR=%LAUNCHER_DIR%\target\leon-exam-launcher-2.0.0.jar"
set "DIST_DIR=%PROJECT_DIR%dist"
set "JPACKAGE_DIR=%DIST_DIR%\jpackage"
set "PAYLOAD_DIR=%DIST_DIR%\windows-payload"
set "RUNTIME_DIR=%DIST_DIR%\runtime-image"
set "PACKAGE_MODE=0"
set "INSTALLER_TYPE=msi"
set "WIN_CONSOLE_OPTION="
set "APP_VERSION=2.0.4"
set "WIN_UPGRADE_UUID=B7049603-F325-4AC9-B9E2-46CA1AA46E95"

echo ==========================================
echo   Leon Exam System Build
echo ==========================================

if "%~1"=="" goto build
if /i "%~1"=="preflight" goto preflight_only
if /i "%~1"=="package" goto package_mode
if /i "%~1"=="package-exe" goto package_exe_mode
if /i "%~1"=="package-debug" goto package_debug_mode

echo ERROR: Unknown command "%~1"
echo Usage:
echo   build.bat
echo   build.bat preflight
echo   build.bat package
echo   build.bat package-exe
echo   build.bat package-debug
exit /b 1

:preflight_only
call :preflight_package
if errorlevel 1 exit /b 1
exit /b 0

:package_mode
set "PACKAGE_MODE=1"
set "INSTALLER_TYPE=msi"
call :preflight_package
if errorlevel 1 exit /b 1
goto build

:package_exe_mode
set "PACKAGE_MODE=1"
set "INSTALLER_TYPE=exe"
call :preflight_package
if errorlevel 1 exit /b 1
goto build

:package_debug_mode
set "PACKAGE_MODE=1"
set "INSTALLER_TYPE=msi"
set "WIN_CONSOLE_OPTION=--win-console"
call :preflight_package
if errorlevel 1 exit /b 1
goto build

:build
rem Step 1: Build frontend
echo.
echo [1/3] Building frontend...
cd /d "%WEB_DIR%" || exit /b 1
if exist "%WEB_DIR%\node_modules\.bin\max.cmd" (
    call "%WEB_DIR%\node_modules\.bin\max.cmd" build
) else if exist "%WEB_DIR%\node_modules\.bin\max" (
    call "%WEB_DIR%\node_modules\.bin\max" build
) else (
    echo ERROR: Frontend dependencies are not installed.
    echo Run:
    echo   cd /d "%WEB_DIR%"
    echo   npm install --registry=https://registry.npmmirror.com
    exit /b 1
)

if errorlevel 1 (
    echo ERROR: Frontend build command failed.
    exit /b 1
)

if not exist "%WEB_DIR%\dist" (
    echo ERROR: Frontend build failed.
    exit /b 1
)

rem Step 2: Copy frontend to Spring Boot static
echo.
echo [2/3] Copying frontend assets...
if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%" || exit /b 1
xcopy /e /i /q "%WEB_DIR%\dist\*" "%STATIC_DIR%\"
if errorlevel 1 (
    echo ERROR: Failed to copy frontend assets.
    exit /b 1
)

rem Step 3: Package backend JAR
echo.
echo [3/3] Packaging backend...
cd /d "%SERVER_DIR%" || exit /b 1
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo ERROR: Backend Maven package failed.
    exit /b 1
)

if not exist "%JAR_PATH%" (
    echo ERROR: JAR build failed: "%JAR_PATH%"
    exit /b 1
)

if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
copy "%JAR_PATH%" "%DIST_DIR%\" >nul
if errorlevel 1 (
    echo ERROR: Failed to copy JAR to dist.
    exit /b 1
)

echo.
echo ==========================================
echo   Build complete.
echo   JAR: %DIST_DIR%\%JAR_NAME%
echo.
echo   Run with:
echo     java -jar %DIST_DIR%\%JAR_NAME% --spring.profiles.active=prod
echo ==========================================

if not "%PACKAGE_MODE%"=="1" (
    endlocal
    exit /b 0
)

echo.
echo Building Windows %INSTALLER_TYPE% installer with launcher, JRE, MariaDB and backend...
echo Package version: %APP_VERSION%

echo.
echo [package] Building launcher...
cd /d "%LAUNCHER_DIR%" || exit /b 1
call mvn clean package -q
if errorlevel 1 (
    echo ERROR: Launcher build failed.
    exit /b 1
)

if not exist "%LAUNCHER_JAR%" (
    echo ERROR: Launcher JAR not found: "%LAUNCHER_JAR%"
    exit /b 1
)

if exist "%PAYLOAD_DIR%" rmdir /s /q "%PAYLOAD_DIR%"
if exist "%JPACKAGE_DIR%" rmdir /s /q "%JPACKAGE_DIR%"
if exist "%RUNTIME_DIR%" rmdir /s /q "%RUNTIME_DIR%"
mkdir "%PAYLOAD_DIR%\app" || exit /b 1
mkdir "%PAYLOAD_DIR%\sql" || exit /b 1

copy "%LAUNCHER_JAR%" "%PAYLOAD_DIR%\launcher.jar" >nul
if errorlevel 1 exit /b 1
copy "%JAR_PATH%" "%PAYLOAD_DIR%\app\%JAR_NAME%" >nul
if errorlevel 1 exit /b 1
xcopy /e /i /q "%PROJECT_DIR%sql" "%PAYLOAD_DIR%\sql" >nul
if errorlevel 1 exit /b 1
xcopy /e /i /q "%MARIADB_SOURCE_DIR%" "%PAYLOAD_DIR%\mariadb" >nul
if errorlevel 1 exit /b 1
mkdir "%JPACKAGE_DIR%" || exit /b 1

echo.
echo [package] Building runtime image with java launcher...
jlink ^
    --add-modules ALL-MODULE-PATH ^
    --no-header-files ^
    --no-man-pages ^
    --compress=2 ^
    --output "%RUNTIME_DIR%"
if errorlevel 1 (
    echo ERROR: jlink runtime image failed.
    exit /b 1
)
if not exist "%RUNTIME_DIR%\bin\java.exe" (
    echo ERROR: Runtime image does not contain java.exe: "%RUNTIME_DIR%\bin\java.exe"
    exit /b 1
)
"%RUNTIME_DIR%\bin\java.exe" --list-modules | findstr /B /C:"java.desktop@" >nul
if errorlevel 1 (
    echo ERROR: Runtime image does not contain java.desktop. Swing launcher cannot show a window.
    exit /b 1
)

jpackage ^
    --name LeonExam ^
    --type "%INSTALLER_TYPE%" ^
    --dest "%JPACKAGE_DIR%" ^
    --input "%PAYLOAD_DIR%" ^
    --runtime-image "%RUNTIME_DIR%" ^
    --main-jar launcher.jar ^
    --main-class com.wts.launcher.LeonExamLauncher ^
    --java-options "-Dfile.encoding=UTF-8" ^
    %WIN_CONSOLE_OPTION% ^
    --win-dir-chooser ^
    --win-shortcut ^
    --win-menu ^
    --win-upgrade-uuid "%WIN_UPGRADE_UUID%" ^
    --app-version "%APP_VERSION%" ^
    --description "Leon Exam System" ^
    --vendor "Leon"
if errorlevel 1 (
    echo ERROR: jpackage failed.
    exit /b 1
)

echo.
echo ==========================================
echo   Windows installer built.
echo   Type: %INSTALLER_TYPE%
echo   Version: %APP_VERSION%
echo   Location: %JPACKAGE_DIR%
echo ==========================================

endlocal
exit /b 0

:preflight_package
echo.
echo [preflight] Checking Windows packaging prerequisites...

where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: java not found. Install JDK 17+ and add it to PATH.
    exit /b 1
)

where jpackage >nul 2>nul
if errorlevel 1 (
    echo ERROR: jpackage not found. Use a full JDK 17+ on the Windows build machine.
    exit /b 1
)

where jlink >nul 2>nul
if errorlevel 1 (
    echo ERROR: jlink not found. Use a full JDK 17+ on the Windows build machine.
    exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo ERROR: mvn not found. Install Maven 3.8+ and add it to PATH.
    exit /b 1
)

where node >nul 2>nul
if errorlevel 1 (
    echo ERROR: node not found. Install Node.js 18+ and add it to PATH.
    exit /b 1
)

if not exist "%WEB_DIR%\node_modules\.bin\max.cmd" if not exist "%WEB_DIR%\node_modules\.bin\max" (
    echo ERROR: Frontend dependencies are not installed.
    echo Run:
    echo   cd /d "%WEB_DIR%"
    echo   npm install --registry=https://registry.npmmirror.com
    exit /b 1
)

where candle.exe >nul 2>nul
if errorlevel 1 (
    echo ERROR: WiX candle.exe not found. Install WiX Toolset 3.x and add it to PATH.
    exit /b 1
)

where light.exe >nul 2>nul
if errorlevel 1 (
    echo ERROR: WiX light.exe not found. Install WiX Toolset 3.x and add it to PATH.
    exit /b 1
)

if not exist "%PROJECT_DIR%sql\init\wts.v1.4.1.sql" (
    echo ERROR: Missing init SQL: "%PROJECT_DIR%sql\init\wts.v1.4.1.sql"
    exit /b 1
)

if not exist "%PROJECT_DIR%sql\migrations\V2_add_point_column.sql" (
    echo ERROR: Missing migration SQL: "%PROJECT_DIR%sql\migrations\V2_add_point_column.sql"
    exit /b 1
)

if not exist "%MARIADB_SOURCE_DIR%\bin\mysqld.exe" (
    echo ERROR: MariaDB mysqld.exe not found.
    echo Put the extracted Windows MariaDB ZIP under:
    echo   "%MARIADB_SOURCE_DIR%"
    exit /b 1
)

if not exist "%MARIADB_SOURCE_DIR%\bin\mysql.exe" (
    echo ERROR: MariaDB mysql.exe not found: "%MARIADB_SOURCE_DIR%\bin\mysql.exe"
    exit /b 1
)

if not exist "%MARIADB_SOURCE_DIR%\bin\mysqladmin.exe" (
    echo ERROR: MariaDB mysqladmin.exe not found: "%MARIADB_SOURCE_DIR%\bin\mysqladmin.exe"
    exit /b 1
)

if not exist "%MARIADB_SOURCE_DIR%\bin\mariadb-install-db.exe" if not exist "%MARIADB_SOURCE_DIR%\bin\mysql_install_db.exe" (
    echo ERROR: MariaDB install-db command not found.
    echo Required one of:
    echo   "%MARIADB_SOURCE_DIR%\bin\mariadb-install-db.exe"
    echo   "%MARIADB_SOURCE_DIR%\bin\mysql_install_db.exe"
    exit /b 1
)

echo [preflight] OK.
exit /b 0
