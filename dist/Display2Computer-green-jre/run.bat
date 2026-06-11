@echo off
setlocal
set "ROOT=%~dp0"
start "Display2Computer" /B javaw -jar "%ROOT%display2computer.jar"
endlocal
