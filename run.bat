@echo off
title Agri Inventory Pro
REM Compiles and starts the application. Requires JDK 11 or newer (javac + java in PATH).
if not exist bin mkdir bin
javac -encoding UTF-8 -cp "lib\*" -d bin src\agri\*.java
if errorlevel 1 (
  echo.
  echo Compile error! Please check that the JDK is installed: run "javac -version"
  pause
  exit /b 1
)
start "" javaw -cp "bin;lib\*" agri.Main
