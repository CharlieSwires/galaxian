@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin
del /q bin\*.class 2>nul
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d bin src\*.java
if errorlevel 1 exit /b 1
jar cfe Galaxian.jar Galaxian -C bin . assets
if errorlevel 1 exit /b 1
echo Built Galaxian.jar
