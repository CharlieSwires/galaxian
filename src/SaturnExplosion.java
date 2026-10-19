/* Copyright © 2026 Charlie's Tenacity Ltd. All rights reserved. */
import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

/** Charles Swires' Saturn explosion, with 3-pixel particles and time-based updates. */
final class SaturnExplosion {
    static final int NO_POINTS = 200;
    static final int PARTICLE_SIZE = 3;
    // Saturn normally advances once every 40 ms (25 frames per second).
    private static final double STEPS_PER_SECOND = 25.0;
    private static final double FADE_PER_SECOND = 50.0;
    final double[] x = new double[NO_POINTS];
    final double[] y = new double[NO_POINTS];
    final double[] dx = new double[NO_POINTS];
    final double[] dy = new double[NO_POINTS];
    private double age;

    SaturnExplosion(double originX, double originY, Random random) {
        for (int i = 0; i < NO_POINTS; i++) {
            x[i] = originX;
            y[i] = originY;
            double bearing = 2.0 * Math.PI * random.nextDouble();
            double speed = random.nextDouble() * 5.0;
            dx[i] = speed * Math.sin(bearing);
            dy[i] = speed * Math.cos(bearing);
        }
    }

    void update(double seconds) {
        if (seconds <= 0) return;
        // Fade twice as fast while keeping the original particle movement speed.
        double elapsed = Math.min(seconds, Math.max(0, 255.0 / FADE_PER_SECOND - age));
        age += elapsed;
        double steps = elapsed * STEPS_PER_SECOND;
        for (int i = 0; i < NO_POINTS; i++) {
            x[i] += dx[i] * steps;
            y[i] += dy[i] * steps;
        }
    }

    int getCount() {
        return Math.max(0, 255 - (int) Math.floor(age * FADE_PER_SECOND + 1e-9));
    }

    void draw(Graphics g) {
        int count = getCount();
        if (count == 0) return;
        g.setColor(new Color(count, count, count));
        for (int i = 0; i < NO_POINTS; i++) {
            g.fillRect((int) x[i] - PARTICLE_SIZE / 2,
                    (int) y[i] - PARTICLE_SIZE / 2, PARTICLE_SIZE, PARTICLE_SIZE);
        }
    }
}
