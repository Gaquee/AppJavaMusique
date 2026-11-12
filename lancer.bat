@echo off
rem Compile puis lance le lecteur. A executer depuis la racine du projet.
setlocal
cd /d "%~dp0"
set FX=lib\javafx-sdk\lib

if not exist out mkdir out
javac -encoding UTF-8 -cp "lib/*" --module-path "%FX%" --add-modules javafx.media -d out src\*.java
if errorlevel 1 (
    echo Echec de la compilation.
    exit /b 1
)
java --enable-native-access=javafx.graphics,javafx.media -cp "out;lib/*" --module-path "%FX%" --add-modules javafx.media App
