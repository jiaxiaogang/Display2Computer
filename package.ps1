$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$JavaHome = "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9"
$Javac = Join-Path $JavaHome "bin\javac.exe"
$Jar = Join-Path $JavaHome "bin\jar.exe"
$Java = Join-Path $JavaHome "bin\java.exe"
$Classes = Join-Path $Root "target\classes"
$Resources = Join-Path $Root "src\main\resources"
$Output = Join-Path $Root "target\show2pc.jar"

Write-Host "Compiling..."
New-Item -ItemType Directory -Force -Path $Classes | Out-Null
$Sources = Get-ChildItem -Path (Join-Path $Root "src\main\java") -Filter *.java -Recurse | ForEach-Object { $_.FullName }
& $Javac -encoding UTF-8 -d $Classes $Sources

Write-Host "Copying resources..."
Copy-Item -Path (Join-Path $Resources "*") -Destination $Classes -Recurse -Force

Write-Host "Creating jar..."
& $Jar cfm $Output (Join-Path $Root "MANIFEST.MF") -C $Classes .

Write-Host "Packaged: $Output"