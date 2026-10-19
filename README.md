# Galaxian — Charlie's Arcade

<img src="Screenshot 2026-10-19 094129.png" width="500" height="500" alt="Paused Screen" title="Navigation Room"/>

A standalone, Galaxian-inspired Java Swing arcade game developed from Charles
Swires' Saturn project. Defend your ship against a coloured alien fleet that
leaves formation, loops into diving attacks and returns for another pass.

This is a new project with its own source, executable JAR, assets and high-score
file. Saturn does not need to be installed and its files are not modified.

## Features

- Animated green scouts, violet escorts and red commanders, using CC0 PNG art.
- A 44-alien formation with a moving starfield behind it.
- Curving dive attacks targeted at your ship; survivors rejoin the formation.
- Enemy bullets travel straight down from a randomly selected living alien.
- Three starting lives and brief protection after losing a life.
- Progressively faster waves and more simultaneous divers.
- Double points for hitting a diving alien.
- An extra life at each 10,000-point milestone.
- Synthesized J2SE sound effects, mute and pause controls.
- Ranked top-ten scores, with insertion between neighbouring scores.
- Swing double buffering, smooth text and window-level key bindings.
- All gameplay updates and rendering run on Swing's event thread. Audio uses a
  separate bounded mixer thread to keep it from blocking the game loop.

This is an independent adaptation, not an exact arcade emulator or official
Namco release. It uses alternative reusable graphics rather than original arcade
sprite rips. There are no shields or mystery saucer in this game.

## Quick start on Windows

1. Extract **Galaxian.zip** into a location of your choice.
2. Open the extracted **Galaxian** folder.
3. Double-click **run.bat**.
4. Press **Enter** at the title screen.

Java 8 or later must be installed and available on PATH. The included
`Galaxian.jar` is already compiled for Java 8, so no build is required to play.
A modern OpenJDK installation is suitable: https://jdk.java.net/

From Command Prompt:

```bat
cd path\to\Galaxian
run.bat
```

From Git Bash, Linux or macOS:

```bash
cd path/to/Galaxian
./run.sh
```

If the extracted scripts lost their executable permission, use `bash run.sh`.
You can also run the JAR directly from the project folder:

```bash
java -jar Galaxian.jar
```

## Controls

| Key | Action |
| --- | --- |
| Enter | Start from the title or high-score screen |
| A / Left arrow | Move left |
| D / Right arrow | Move right |
| Space | Fire; hold for repeated shots, with at most two on screen |
| P / Escape | Pause or resume gameplay |
| M | Mute/unmute sound |
| T | Play a sound test; unmute and retry output if unavailable |
| H | Show scores from title, or return from scores to title |
| Escape on the score screen | Return to title |

Switching to another window pauses an active game and releases held controls.
Return to the game and press P or Escape to resume. Game controls are disabled
while entering a high-score name. Name entry is part of the game window, without
a separate modal dialog. After saving, press Enter on the score table to start
the next game. The Enter release used to submit a name cannot start it early.

## Scoring

| Alien | In formation | While diving |
| --- | ---: | ---: |
| Green scout | 50 | 100 |
| Violet escort | 80 | 160 |
| Red commander | 150 | 300 |

The score table retains the best ten results. When it is full, you must beat its
lowest score. A newly entered tie appears below older equal scores. Enter a name in the in-window form and press Enter or click **SAVE SCORE**.
Leaving the name blank records the qualifying score as **PLAYER**. Names are limited
to 20 printable ASCII characters; delimiters used by the save format are cleaned.

Scores are stored in **galaxian-scores.bin**, relative to the working directory.
The launch scripts set this to the project folder. The file is created on the
first saved result. It uses Saturn's XOR sequence (seed 73890) and compatible
`ScoreName` row layout, with UTF-8/ASCII-safe names. This is obfuscation, not
secure encryption. Saturn's `scores.bin` is neither imported nor overwritten.

If a score file cannot be parsed, it is left unchanged and saving is disabled
for that session. Rename it to a backup before starting again if you want a fresh
table. Keep a copy before editing it or moving scores between projects.

## Build from source

Install an OpenJDK **JDK** (not just a runtime) with `javac` and `jar` on PATH.
There is no Maven, Gradle or third-party runtime dependency.

Windows:

```bat
build.bat
run.bat
```

Git Bash / Linux / macOS:

```bash
./build.sh
./run.sh
```

Manual compilation:

```bash
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d bin src/*.java
jar cfe Galaxian.jar Galaxian -C bin . assets
```

With JDK 9 or later, `--release 8` can replace the source/target options to check
against the Java 8 API as well. The supplied JAR was built with `--release 8`.
The assets are embedded in the JAR so it can load its graphics without loose
files. After changing images, rebuild the JAR or run the classes directly:

```bash
java -cp bin Galaxian
```

## Eclipse

Use **File → Import → General → Existing Projects into Workspace**, select the
extracted Galaxian folder, then finish. The ZIP includes `.project` and
`.classpath`. Configure a JDK 8 or later, refresh the project and build.

Run `Galaxian` as a **Java Application**. Set the working directory in the run
configuration to the **Galaxian project folder**, so the loose assets can be
found and saved scores go into that folder. This project uses the classpath;
there is no `module-info.java`.

## Graphics and replacing the aliens

The bundled artwork is **Alien Set** by **The_Scientist___**, available under
CC0 at https://opengameart.org/content/alien-set. See `ASSET-CREDITS.md` for source
links and attribution. All 17 original PNGs are included.

Java's `ImageIO` supports PNG, JPEG, GIF, BMP and WBMP in a standard installation.
**PNG is used here for lossless pixels and transparent backgrounds.** JPEG
cannot retain that transparency. The game animates sprite sheets itself rather
than relying on animated GIF playback.

`Sprites.java` loads these sheets:

| Role | File in `assets/aliens/` | Sheet layout |
| --- | --- | --- |
| Scout | `alien1_g.png` | 3 × 3 frames, each 32 × 32 pixels |
| Escort | `alien2_p.png` | 3 × 3 frames, each 32 × 32 pixels |
| Commander | `alien3_r.png` | 2 × 2 frames, each 32 × 32 pixels |

Frames are read left-to-right, then top-to-bottom. To swap colours, change the
filenames in `Sprites.java` to the supplied `_g`, `_p`, `_r` or `_y` variants.
Keep the expected grid dimensions. Rebuild afterwards.

## Tests and verification

```bat
test.bat
```

or:

```bash
./test.sh
```

These dependency-free, headless tests cover formation size, movement limits,
shot limits, kills and scoring, diver return, player damage and protection,
game over and restart, wave progression, bonus lives, top-ten insertion/ties,
saving/loading, XOR round trips, key actions and actual PNG/rendering paths.
They also run a deterministic simulation to check positions and collection use.

Preview renders are written under `build/`. The ZIP includes selected renders
under `docs/` for a quick visual reference. Automated model and render checks
passed in the build environment. Audible output and interactive Windows focus
still need checking on your computer; the build environment has no sound device
or interactive desktop.

## Javadoc

From the project folder with a JDK:

```bash
javadoc -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -private -d javadoc src/*.java
```

Open `javadoc/index.html`. Missing-comment warnings do not generally stop output.

## Files

```text
Galaxian/
  README.md
  ASSET-CREDITS.md
  Galaxian.jar
  build.bat / build.sh
  run.bat / run.sh
  test.bat / test.sh
  src/
    Galaxian.java
    GamePanel.java
    GameModel.java
    Sprites.java
    HighScores.java
    SoundEffects.java
    SaturnExplosion.java
  assets/aliens/
  tests/
  docs/
  bin/
```

`SoundEffects.java` is adapted from the supplied Saturn project. The gameplay
and rendering are rebuilt for Galaxian-style attacks and Swing event-thread
ownership, rather than copying Saturn's manual frame-painting loop.

## Troubleshooting

- **Java is not recognised:** install OpenJDK, put its `bin` directory on PATH,
  and open a new terminal.
- **No sound:** press M, check the system mixer and chosen output device. If Java
  cannot open an audio line, it prints one message and continues silently.
- **Game appears frozen after changing windows:** press P to resume; focus loss
  intentionally pauses gameplay.
- **Graphics cannot be found from Eclipse:** set the working directory to the
  project folder. The prebuilt JAR also contains the same assets.
- **Scores do not persist:** use a writable project folder and read any warning
  displayed at the bottom of the window.

## Rights

Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved for project code.
The included third-party alien art is separately available under CC0.
See `ASSET-CREDITS.md`. No original Galaxian ROMs, graphics or recordings are
included; this is an independent inspired project.

## Keyboard and Saturn explosion update

High-score name entry now uses an in-window Swing form. Saving explicitly exits
the entry state, clears pause/held-control flags and returns focus to the game.
The regression tests submit names with both Enter and the button, then start a
new game and verify movement and firing. No modal focus handoff is required.

Explosions now use Charles Swires' original Saturn algorithm in
`SaturnExplosion.java`: 200 particles, uniformly random bearing, speed selected
from 0 to 5, `dx = speed * sin(bearing)` and `dy = speed * cos(bearing)`.
Brightness fades from 255 to 0 in grayscale. Particles are drawn as 3 × 3 pixels
instead of single pixels. Motion uses Saturn's normal 25 updates per second. Brightness now fades at
50 levels per second (twice the original rate), independently of the monitor
frame rate. The fade now lasts approximately 5.1 seconds; off-screen
particles are naturally clipped by the game panel.

## Audio output update

Audio now tries the original Saturn 22,050 Hz mono format first, followed by
stereo and 44,100/48,000 Hz fallbacks, using the default output and then other
available Java mixers. Driver gain/volume controls are left at their established
defaults. The game shows READY,
STARTING, MUTED or UNAVAILABLE instead of claiming sound is on when opening failed.

Press **T** on the title screen or during gameplay for an ascending test sound.
It unmutes playback and retries an unavailable device. Device attempts and errors
are written to **sound-diagnostics.txt** in the project folder. If it still stays
silent, check Windows **Settings → System → Sound → Volume mixer**, including the
Java application's volume/output, and supply `sound-diagnostics.txt` for diagnosis.
A READY status means Java opened and started a line; it cannot verify that the
Windows mixer, speakers or headphones are audible.

## Controls between waves

Movement and fire key presses/releases are tracked through the wave-clear delay,
so holding Space or a movement key into the next wave continues to work. Keys
released during the delay are also cleared correctly. Window focus is requested
when the next wave starts without wiping held keys. Pausing, starting a new game,
name entry and losing window focus still clear controls appropriately.

The log also records this audio build's code location, sound request count,
last requested effect, non-silent PCM frame count and bytes written. These
counters distinguish a missing game event from an opened output receiving audio.
They are refreshed approximately once a second while the output is active.
