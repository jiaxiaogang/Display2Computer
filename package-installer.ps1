$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$JavaHome = "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9"
$JPackage = Join-Path $JavaHome "bin\jpackage.exe"
$Dist = Join-Path $Root "dist"
$InstallerName = "Display2Computer-0.1.0.exe"

if (-not (Test-Path $JPackage)) {
    throw "jpackage not found: $JPackage"
}

if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
    throw "WiX Toolset 3.x not found. Install WiX first, then reopen PowerShell so candle.exe and light.exe are on PATH."
}

Write-Host "Building jar..."
& (Join-Path $Root "package.ps1")

Write-Host "Creating Windows installer..."
New-Item -ItemType Directory -Force -Path $Dist | Out-Null
$ExistingInstaller = Join-Path $Dist $InstallerName
if (Test-Path $ExistingInstaller) {
    Remove-Item $ExistingInstaller -Force
}

& $JPackage `
    --type exe `
    --name Display2Computer `
    --input (Join-Path $Root "target") `
    --main-jar display2computer.jar `
    --main-class show2pc.Main `
    --dest $Dist `
    --app-version 0.1.0 `
    --vendor Display2Computer `
    --win-menu `
    --win-shortcut

Write-Host "Installer created in: $Dist"
Write-Host "Look for Display2Computer-0.1.0.exe or the generated Display2Computer installer executable."
