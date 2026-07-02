# Compiles the project using JavaFX jars from the local Maven repository.
$ErrorActionPreference = "Stop"

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

New-Item -ItemType Directory -Force -Path "$PSScriptRoot\target\classes" | Out-Null

$sources = Get-ChildItem -Recurse -Filter *.java "$PSScriptRoot\src\main\java" | ForEach-Object { $_.FullName }
Write-Host "Compiling $($sources.Count) source files..."
javac -cp $cp -d "$PSScriptRoot\target\classes" $sources
Write-Host "Compile OK."
