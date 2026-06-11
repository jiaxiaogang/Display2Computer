@echo off
taskkill /IM javaw.exe /F >nul 2>nul
if errorlevel 1 (
  echo No javaw.exe process was running.
) else (
  echo Display2Computer background process stopped.
)
pause
