/*==========================================================================
Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved.

This source code is proprietary and confidential. Unauthorised copying, modification, distribution or use is prohibited.
============================================================================*/
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Small, asynchronous J2SE PCM mixer. No external libraries or sound files. */
public final class SoundEffects implements AutoCloseable {
    public enum Effect { SHOT, ENEMY_SHOT, EXPLOSION, PLAYER_HIT, SHIELD_HIT,
        MARCH, BONUS, BONUS_HIT, LEVEL_UP, GAME_OVER }
    private static final int RATE = 22050;
    private final EnumMap<Effect, short[]> samples = new EnumMap<>(Effect.class);
    private final ArrayBlockingQueue<Effect> pending = new ArrayBlockingQueue<>(32);
    private volatile boolean running = true;
    private volatile boolean muted;
    private volatile SourceDataLine line;
    private final Thread worker;
    private volatile String status = "STARTING";
    private final StringBuilder diagnostics = new StringBuilder();
    private final java.util.concurrent.atomic.AtomicLong requests = new java.util.concurrent.atomic.AtomicLong();
    private long nonSilentFrames, bytesWritten;
    private volatile Effect lastRequested;
    private final java.util.concurrent.Callable<SourceDataLine> testOutput;

    public SoundEffects() { this(null); }

    // Allows the complete mixer to be tested without speakers or a device driver.
    SoundEffects(java.util.concurrent.Callable<SourceDataLine> testOutput) {
        this.testOutput = testOutput;
        diagnostics.append("Galaxian audio build: mono-first / playback counters\n");
        diagnostics.append("Code location: ").append(SoundEffects.class.getProtectionDomain().getCodeSource().getLocation()).append('\n');
        for (Effect effect : Effect.values()) samples.put(effect, synthesize(effect));
        worker = new Thread(this::mix, "galaxian-audio");
        worker.setDaemon(true);
        worker.start();
    }

    /** Never waits for the audio device; excess requests are dropped. */
    public void play(Effect effect) {
        requests.incrementAndGet();lastRequested=effect;
        if (running && !muted) pending.offer(effect);
    }

    public void toggleMute() {
        muted = !muted;
        pending.clear();
    }

    public String status() { return status; }

    private SourceDataLine openOutput() throws Exception {
        if (testOutput != null) return testOutput.call();
        // Restore the original Saturn mono format first, with standard fallbacks.
        for (int rate : new int[] {22050, 44100, 48000}) {
            for (int channels : new int[] {1, 2}) {
                AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
                SourceDataLine output = tryOutput(format, null);
                if (output != null) return output;
                for (javax.sound.sampled.Mixer.Info mixer : AudioSystem.getMixerInfo()) {
                    output = tryOutput(format, mixer);
                    if (output != null) return output;
                }
            }
        }
        throw new javax.sound.sampled.LineUnavailableException("No supported PCM output could be opened");
    }

    private SourceDataLine tryOutput(AudioFormat format, javax.sound.sampled.Mixer.Info mixer) {
        if (!running) return null;
        SourceDataLine output = null;
        String device = mixer == null ? "system default" : mixer.getName();
        try {
            output = mixer == null ? AudioSystem.getSourceDataLine(format)
                    : AudioSystem.getSourceDataLine(format, mixer);
            output.open(format, format.getFrameSize() * 2048);
            if (!running) { output.close(); return null; }
            // Leave driver gain/volume controls at their established defaults.
            diagnostics.append("Opened ").append(device).append(": ").append(format).append('\n');
            return output;
        } catch (Exception ex) {
            if (output != null) output.close();
            diagnostics.append("Could not open ").append(device).append(": ").append(format)
                    .append(" — ").append(ex).append('\n');
            return null;
        }
    }

    private void saveDiagnostics() {
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("sound-diagnostics.txt"),
                    (diagnostics.toString() + "Sound requests: " + requests.get()
                    + "\nLast requested effect: " + lastRequested
                    + "\nNon-silent PCM frames: " + nonSilentFrames
                    + "\nBytes written: " + bytesWritten
                    + "\nMuted: " + muted + "\nStatus: " + status + "\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ex) { System.err.println("Cannot write sound diagnostics: " + ex.getMessage()); }
    }

    /** Little-endian 16-bit PCM, duplicating the sample into each channel. */
    static void writeFrame(byte[] block, int offset, int sample, int channels) {
        int value = Math.max(-32768, Math.min(32767, sample));
        for (int channel = 0; channel < channels; channel++) {
            block[offset + channel * 2] = (byte) value;
            block[offset + channel * 2 + 1] = (byte) (value >> 8);
        }
    }

    private static final class Voice {
        final short[] data;
        int position;
        Voice(short[] data) { this.data = data; }
    }

    private void mix() {
        List<Voice> voices = new ArrayList<>();
        try {
            SourceDataLine output = openOutput();
            line = output;
            if (!running) return;
            AudioFormat format = output.getFormat();
            int channels = format.getChannels();
            for (Effect effect : Effect.values()) samples.put(effect, synthesize(effect, (int) format.getSampleRate()));
            output.start();
            status = "READY";
            if(testOutput==null)saveDiagnostics();
            int frameSize = channels * 2;
            byte[] block = new byte[256 * frameSize];
            long nextLog=System.nanoTime()+1000000000L;
            while (running) {
                if (muted) { voices.clear(); pending.clear(); }
                Effect effect;
                while ((effect = pending.poll()) != null) {
                    if (!muted && voices.size() < 16) voices.add(new Voice(samples.get(effect)));
                }
                for (int i = 0; i < block.length; i += frameSize) {
                    int value = 0;
                    Iterator<Voice> it = voices.iterator();
                    while (it.hasNext()) {
                        Voice voice = it.next();
                        value += voice.data[voice.position++];
                        if (voice.position == voice.data.length) it.remove();
                    }
                    if(value!=0)nonSilentFrames++;
                    writeFrame(block, i, value, channels);
                }
                int offset = 0;
                while (running && offset < block.length) {
                    int written = output.write(block, offset, block.length - offset);
                    if (written <= 0) break;
                    offset += written;bytesWritten += written;
                }
                if(System.nanoTime()>=nextLog) {
                    if(testOutput==null)saveDiagnostics();
                    nextLog=System.nanoTime()+1000000000L;
                }
            }
        } catch (Exception ex) {
            if (running) {
                status = "UNAVAILABLE";
                diagnostics.append("Audio failed: ").append(ex).append('\n');
                System.err.println("Sound unavailable: " + ex.getMessage() + ". See sound-diagnostics.txt");
                saveDiagnostics();
            }
        } finally {
            running = false;
            pending.clear();
            SourceDataLine output = line;
            if (output != null) output.close();
            if(testOutput==null)saveDiagnostics();
        }
    }

    /** Generates short retro effects with attack/release envelopes to avoid clicks. */
    static short[] synthesize(Effect effect) { return synthesize(effect, RATE); }

    static short[] synthesize(Effect effect, int sampleRate) {
        double duration = 0.12, start = 600, end = 120, noise = 0;
        switch (effect) {
        case SHOT: start = 1400; end = 200; break;
        case ENEMY_SHOT: duration = 0.09; start = 280; end = 90; break;
        case EXPLOSION: duration = 0.28; start = 150; end = 35; noise = 0.8; break;
        case PLAYER_HIT: duration = 0.55; start = 220; end = 30; noise = 0.7; break;
        case SHIELD_HIT: duration = 0.05; start = 400; end = 70; noise = 0.8; break;
        case MARCH: duration = 0.045; start = 90; end = 60; break;
        case BONUS: duration = 0.18; start = 500; end = 850; break;
        case BONUS_HIT: duration = 0.4; start = 600; end = 1800; break;
        case LEVEL_UP: duration = 0.65; start = 400; end = 1600; break;
        case GAME_OVER: duration = 0.85; start = 600; end = 70; break;
        default: break;
        }
        short[] result = new short[(int) (sampleRate * duration)];
        Random random = new Random(1234 + effect.ordinal());
        double phase = 0;
        for (int i = 0; i < result.length; i++) {
            double progress = (double) i / result.length;
            double frequency = start + (end - start) * progress;
            phase += 2 * Math.PI * frequency / sampleRate;
            double envelope = Math.min(1, i / (sampleRate * 0.005)) * Math.pow(1 - progress, 1.5);
            double wave = (1 - noise) * Math.sin(phase) + noise * (random.nextDouble() * 2 - 1);
            result[i] = (short) (wave * envelope * 6500);
        }
        return result;
    }

    @Override public void close() {
        running = false;
        status = "CLOSED";
        pending.clear();
        SourceDataLine output = line;
        if (output != null) { output.stop(); output.flush(); output.close(); }
        worker.interrupt();
    }
}
