$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$JavaHome = "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9"
$JPackage = Join-Path $JavaHome "bin\jpackage.exe"
$Dist = Join-Path $Root "dist"
$PackageDir = Join-Path $Dist "Display2Computer"

if (-not (Test-Path $JPackage)) {
    throw "jpackage not found: $JPackage"
}

Write-Host "Building jar..."
& (Join-Path $Root "package.ps1")

Write-Host "Creating portable package with bundled runtime..."
if (Test-Path $PackageDir) {
    Remove-Item $PackageDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $Dist | Out-Null

& $JPackage `
    --type app-image `
    --name Display2Computer `
    --input (Join-Path $Root "target") `
    --main-jar display2computer.jar `
    --main-class show2pc.Main `
    --dest $Dist `
    --app-version 0.1.0 `
    --vendor Display2Computer

Write-Host "Portable package created: $PackageDir"
