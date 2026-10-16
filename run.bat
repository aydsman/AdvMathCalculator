@echo off
rem Builds and launches the calculator. Requires JDK 21+ on PATH or JAVA_HOME.
cd /d "%~dp0"
call mvnw.cmd -q javafx:run
if errorlevel 1 pause
