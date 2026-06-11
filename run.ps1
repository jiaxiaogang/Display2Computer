$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$JavaHome = "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9"
$Javac = Join-Path $JavaHome "bin\javac.exe"
$Java = Join-Path $JavaHome "bin\java.exe"
$Classes = Join-Path $Root "target\classes"

if (!(Test-Path $Javac)) {
    throw "javac not found: $Javac"
}

New-Item -ItemType Directory -Force -Path $Classes | Out-Null

$Sources = Get-ChildItem -Path (Join-Path $Root "src\main\java") -Filter *.java -Recurse | ForEach-Object { $_.FullName }
& $Javac -encoding UTF-8 -d $Classes $Sources

& $Java -cp "$Classes;$Root\src\main\resources" show2pc.Main
