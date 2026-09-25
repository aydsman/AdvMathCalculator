# Compiles the project using JavaFX jars from the local Maven repository.
$ErrorActionPreference = "Stop"

function Require-Command($name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Could not find '$name' on PATH. Install JDK 21+ and make sure java/javac are available."
    }
}

Require-Command java
Require-Command javac

$javaVer = & java -version 2>&1 | Out-String
Write-Host "Using Java:"
Write-Host $javaVer

$ver = "21.0.5"
$jfx = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"
$jars = @(
    "$jfx\javafx-base\$ver\javafx-base-$ver.jar",
    "$jfx\javafx-base\$ver\javafx-base-$ver-win.jar",
    "$jfx\javafx-graphics\$ver\javafx-graphics-$ver.jar",
    "$jfx\javafx-graphics\$ver\javafx-graphics-$ver-win.jar",
    "$jfx\javafx-controls\$ver\javafx-controls-$ver.jar",
    "$jfx\javafx-controls\$ver\javafx-controls-$ver-win.jar"
)

$missing = @($jars | Where-Object { -not (Test-Path $_) })
if ($missing.Count -gt 0) {
    Write-Host "JavaFX jars missing from local Maven cache. Trying 'mvn dependency:resolve'..."
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Write-Host ""
        Write-Host "Missing jars:"
        $missing | ForEach-Object { Write-Host "  $_" }
        throw @"
JavaFX is not downloaded yet, and Maven (mvn) was not found on PATH.

Fix (pick one):
  1) Install Maven, then from the project root run:
       mvn dependency:resolve
     then run .\run.bat again
  2) Or install the Extension Pack for Java in VS Code/Cursor, open this folder,
     and Run ui.Launcher from the Java extension (not Code Runner).
"@
    }
    Push-Location $PSScriptRoot
    try {
        & mvn -q dependency:resolve
    } finally {
        Pop-Location
    }
    $missing = @($jars | Where-Object { -not (Test-Path $_) })
    if ($missing.Count -gt 0) {
        Write-Host "Still missing:"
        $missing | ForEach-Object { Write-Host "  $_" }
        throw "Maven finished but JavaFX win jars are still missing. Check your network / Maven settings."
    }
}

$cp = ($jars -join ';')
$outDir = Join-Path $PSScriptRoot "target\classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$sources = @(Get-ChildItem -Recurse -Filter *.java (Join-Path $PSScriptRoot "src\main\java") | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) {
    throw "No .java sources found under src\main\java"
}

Write-Host "Compiling $($sources.Count) source files..."
& javac -encoding UTF-8 -cp $cp -d $outDir @sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}
Write-Host "Compile OK."
