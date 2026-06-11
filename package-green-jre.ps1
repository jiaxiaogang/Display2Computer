$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$PackageDir = Join-Path $Root "dist\Display2Computer-green-jre"

Write-Host "Building jar..."
& (Join-Path $Root "package.ps1")

Write-Host "Creating JDK-dependent green package..."
if (Test-Path $PackageDir) {
    Remove-Item $PackageDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $PackageDir | Out-Null

Copy-Item -Path (Join-Path $Root "target\display2computer.jar") -Destination $PackageDir -Force
Copy-Item -Path (Join-Path $Root "run-jar.ps1") -Destination $PackageDir -Force
Copy-Item -Path (Join-Path $Root "PCStop.bat") -Destination $PackageDir -Force
Copy-Item -Path (Join-Path $Root "README.md") -Destination $PackageDir -Force

$RunBat = @'
@echo off
setlocal
set "ROOT=%~dp0"
start "Display2Computer" /B javaw -jar "%ROOT%display2computer.jar"
endlocal
'@
Set-Content -Path (Join-Path $PackageDir "run.bat") -Value $RunBat -Encoding ASCII

$RunSilent = @'
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
jar = root & "\display2computer.jar"
shell.CurrentDirectory = root
shell.Run "javaw -jar " & Chr(34) & jar & Chr(34), 0, False
'@
Set-Content -Path (Join-Path $PackageDir "PCRun.vbs") -Value $RunSilent -Encoding ASCII

Write-Host "Green package created: $PackageDir"
