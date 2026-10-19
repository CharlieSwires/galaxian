@echo off
setlocal
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
if not exist build\test-classes mkdir build\test-classes
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -cp bin -d build\test-classes tests\*.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "bin;build\test-classes" GalaxianTests

if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "bin;build\test-classes" ScoreEntryAndExplosionTests

if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "bin;build\test-classes" AudioTests

if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "bin;build\test-classes" WaveTransitionTests

if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "bin;build\test-classes" MixerPlaybackTests

if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "bin;build\test-classes" AimedShotTests
