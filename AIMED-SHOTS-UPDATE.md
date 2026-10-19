Corrected combined aimed-shot update
===================================
Close the game. Extract this ZIP into your existing Galaxian project folder,
allowing replacement. ZIP paths begin with src/, bin/ and Galaxian.jar.
Run ./build.sh in Git Bash (or build.bat in Command Prompt), then run.bat.
In Eclipse, refresh the project and clean/build before launching.

The first aimed-shot copy-over was based on an older GameModel that used Burst,
which conflicted with the current renderer's SaturnExplosion. This corrected
bundle includes one consistent set of all production source files, rebuilt
classes and the executable JAR.

Combined features retained:
- Aimed bullets from descending divers, random spread, fixed non-homing direction.
- Saturn's 200-particle explosions with 3x3 pixels and the faster 5.1-second fade.
- In-window name entry and keyboard recovery after scores and wave transitions.
- Current sound output and diagnostics.

The build scripts remove stale compiled classes before rebuilding. Source,
JAR and all regression tests build and pass together. Your saved scores and
artwork are not included or overwritten. Interactive Windows play still needs
checking locally.
