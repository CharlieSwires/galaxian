#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
mkdir -p bin
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d bin src/*.java
jar cfe Galaxian.jar Galaxian -C bin . assets
printf 'Built Galaxian.jar\n'
