@echo off
echo === TitleMod Security Build ===
echo.
echo Cleaning previous builds...
call gradlew clean
echo.
echo Building with security enhancements...
call gradlew build shadowJar proguard
echo.
echo === Build Complete ===
echo.
echo Production JAR: build\libs\titlemod-1.0.0-SECURE-obfuscated.jar
echo Shadow JAR: build\libs\titlemod-1.0.0-SECURE-shadow.jar
echo.
echo Press any key to exit...
pause >nul