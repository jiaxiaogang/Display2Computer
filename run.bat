@echo off
setlocal

set "ROOT=%~dp0"
set "JAVAW=C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9\bin\javaw.exe"
set "JAR=%ROOT%target\display2computer.jar"

if not exist "%JAVAW%" exit /b 1
if not exist "%JAR%" exit /b 1

cd /d "%ROOT%"
start "Display2Computer" /B "%JAVAW%" -jar "%JAR%"
exit /b 0
