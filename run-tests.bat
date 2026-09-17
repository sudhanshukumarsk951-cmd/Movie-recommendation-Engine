@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\movieengine\*.java test\RecommendationEngineTest.java test\ProjectSmokeTest.java
if errorlevel 1 (
  echo.
  echo Compilation failed. Fix the errors above.
  pause
  exit /b 1
)
java -cp out RecommendationEngineTest
if errorlevel 1 (pause & exit /b 1)
java -cp out ProjectSmokeTest
if errorlevel 1 (pause & exit /b 1)
echo.
echo ALL TESTS PASSED.
pause
