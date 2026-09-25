@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"
if errorlevel 1 (
  echo.
  echo Run failed. Do NOT use Code Runner on Launcher.java.
  echo From this folder, use:  run.bat
  echo Or in Cursor/VS Code: install Extension Pack for Java, then Run ui.Launcher.
  pause
  exit /b 1
)
