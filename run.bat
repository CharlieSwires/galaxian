@echo off
setlocal
cd /d "%~dp0"
java -jar Galaxian.jar %*
if errorlevel 1 pause
