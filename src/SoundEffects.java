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

    public SoundEffects() {
        for (Effect effect : Effect.values()) samples.put(effect, synthesize(effect));
        worker = new Thread(this::mix, "galaxian-audio");
        worker.setDaemon(true);
        worker.start();
    }

    /** Never waits for the audio device; excess requests are dropped. */
    public void play(Effect effect) {
        if (running && !muted) pending.offer(effect);
    }

    public void toggleMute() {
        muted = !muted;
        pending.clear();
    }

    private static final class Voice {
        final short[] data;
        int position;
        Voice(short[] data) { this.data = data; }
    }

    private void mix() {
        List<Voice> voices = new ArrayList<>();
        try {
            AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
            SourceDataLine output = AudioSystem.getSourceDataLine(format);
            line = output;
            if (!running) return;
            output.open(format, 4096);
            output.start();
            byte[] block = new byte[512];
            while (running) {
                if (muted) { voices.clear(); pending.clear(); }
                Effect effect;
                while ((effect = pending.poll()) != null) {
                    if (!muted && voices.size() < 16) voices.add(new Voice(samples.get(effect)));
                }
                for (int i = 0; i < block.length; i += 2) {
                    int value = 0;
                    Iterator<Voice> it = voices.iterator();
                    while (it.hasNext()) {
                        Voice voice = it.next();
                        value += voice.data[voice.position++];
                        if (voice.position == voice.data.length) it.remove();
                    }
                    value = Math.max(-32768, Math.min(32767, value));
                    block[i] = (byte) value;
                    block[i + 1] = (byte) (value >> 8);
                }
                int offset = 0;
                while (running && offset < block.length) {
                    int written = output.write(block, offset, block.length - offset);
                    if (written <= 0) break;
                    offset += written;
                }
            }
        } catch (Exception ex) {
            if (running) System.err.println("Sound unavailable; continuing silently: " + ex.getMessage());
        } finally {
            running = false;
            pending.clear();
            SourceDataLine output = line;
            if (output != null) output.close();
        }
    }

    /** Generates short retro effects with attack/release envelopes to avoid clicks. */
    static short[] synthesize(Effect effect) {
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
        short[] result = new short[(int) (RATE * duration)];
        Random random = new Random(1234 + effect.ordinal());
        double phase = 0;
        for (int i = 0; i < result.length; i++) {
            double progress = (double) i / result.length;
            double frequency = start + (end - start) * progress;
            phase += 2 * Math.PI * frequency / RATE;
            double envelope = Math.min(1, i / (RATE * 0.005)) * Math.pow(1 - progress, 1.5);
            double wave = (1 - noise) * Math.sin(phase) + noise * (random.nextDouble() * 2 - 1);
            result[i] = (short) (wave * envelope * 6500);
        }
        return result;
    }

    @Override public void close() {
        running = false;
        pending.clear();
        SourceDataLine output = line;
        if (output != null) { output.stop(); output.flush(); output.close(); }
        worker.interrupt();
    }
}
