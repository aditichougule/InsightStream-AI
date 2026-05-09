@echo off
REM AI Video Intelligence Platform - Quick Start Script for Windows

echo.
echo 🚀 AI Video Intelligence Platform - Quick Start
echo ================================================
echo.

REM Check Java
echo ✓ Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java not found. Please install Java 17+
    pause
    exit /b 1
)
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| find "version"') do set JAVA_VERSION=%%i
echo   Using Java %JAVA_VERSION%
echo.

REM Check Maven
echo ✓ Checking Maven...
mvn -v >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven not found. Please install Maven 3.8+
    pause
    exit /b 1
)
for /f %%i in ('mvn -v ^| findstr /R "Apache Maven"') do set MVN_VERSION=%%i
echo   %MVN_VERSION%
echo.

REM Check PostgreSQL
echo ✓ Checking PostgreSQL...
psql -U postgres -d postgres -c "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo ⚠️  PostgreSQL connection failed. Please ensure PostgreSQL is running
    echo    Download: https://www.postgresql.org/download/
    pause
    exit /b 1
)
echo   PostgreSQL is running
echo.

REM Build project
echo ✓ Building project...
call mvn clean package -q -DskipTests
if errorlevel 1 (
    echo ❌ Build failed
    pause
    exit /b 1
)
echo   ✓ Build successful
echo.

REM Start application
echo 🎉 Starting AI Video Intelligence Platform...
echo.
echo 📱 Application running at: http://localhost:8080
echo 📚 API Documentation: http://localhost:8080/swagger-ui/index.html
echo.
echo Press Ctrl+C to stop the application
echo.

call mvn spring-boot:run
pause
