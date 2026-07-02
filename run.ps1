# Compiles and runs the Advanced Math Calculator without needing Maven on PATH.
$ErrorActionPreference = "Stop"

& "$PSScriptRoot\compile.ps1"

$ver = "21.0.5"
$jfx = "$env:USERPROFILE\.m2\repository\org\openjfx"
$jars = @(
    "$jfx\javafx-base\$ver\javafx-base-$ver.jar",
    "$jfx\javafx-base\$ver\javafx-base-$ver-win.jar",
    "$jfx\javafx-graphics\$ver\javafx-graphics-$ver.jar",
    "$jfx\javafx-graphics\$ver\javafx-graphics-$ver-win.jar",
    "$jfx\javafx-controls\$ver\javafx-controls-$ver.jar",
    "$jfx\javafx-controls\$ver\javafx-controls-$ver-win.jar"
)
$cp = ($jars -join ';')

$runCp = "$cp;$PSScriptRoot\target\classes;$PSScriptRoot\src\main\resources"
Write-Host "Launching..."
java -cp $runCp ui.Launcher
