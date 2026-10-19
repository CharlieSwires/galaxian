#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
./build.sh
mkdir -p build/test-classes
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -cp bin -d build/test-classes tests/*.java
# Windows Java uses a semicolon; Linux/macOS Java uses a colon.
separator=':'
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) separator=';' ;; esac
java -Djava.awt.headless=true -cp "bin${separator}build/test-classes" GalaxianTests

java -Djava.awt.headless=true -cp "bin${separator}build/test-classes" ScoreEntryAndExplosionTests

java -Djava.awt.headless=true -cp "bin${separator}build/test-classes" AudioTests

java -Djava.awt.headless=true -cp "bin${separator}build/test-classes" WaveTransitionTests

java -Djava.awt.headless=true -cp "bin${separator}build/test-classes" MixerPlaybackTests
