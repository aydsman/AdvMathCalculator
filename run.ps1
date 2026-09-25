# Compiles and runs the Advanced Math Calculator.
# Usage (from project root):  .\run.bat   or   .\run.ps1
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot
& "$PSScriptRoot\compile.ps1"

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
$cp = ($jars -join ';')

$classes = Join-Path $PSScriptRoot "target\classes"
$resources = Join-Path $PSScriptRoot "src\main\resources"
$runCp = "$cp;$classes;$resources"

Write-Host "Launching ui.Launcher..."
& java -cp $runCp ui.Launcher
if ($LASTEXITCODE -ne 0) {
    throw "java exited with code $LASTEXITCODE"
}
