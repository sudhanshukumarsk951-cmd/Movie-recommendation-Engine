@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src\movieengine\*.java
if errorlevel 1 (
  echo.
  echo Compilation failed. Fix the errors above.
  pause
  exit /b 1
)
java -cp out movieengine.Main
