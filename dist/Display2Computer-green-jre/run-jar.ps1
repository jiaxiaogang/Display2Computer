$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Java = "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9\bin\java.exe"
$Jar = Join-Path $Root "target\display2computer.jar"

if (!(Test-Path $Jar)) {
    & (Join-Path $Root "package.ps1")
}

& $Java -jar $Jar
