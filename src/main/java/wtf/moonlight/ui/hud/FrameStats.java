package wtf.moonlight.ui.hud;

import java.util.Arrays;

final class FrameStats {
    private static final float WINDOW = 5;
    private static final float PERIOD = 0.5F;

    private final float[] times = new float[4096];
    private final float[] sorted = new float[times.length];
    private int head;
    private int count;
    private float elapsed = PERIOD;
    private int fps;
    private int low;

    void add(float delta) {
        times[head] = delta;
        head = (head + 1) % times.length;
        count = Math.min(count + 1, times.length);
        elapsed += delta;
        if (elapsed < PERIOD) return;
        elapsed = 0;
        float total = 0;
        float second = 0;
        int frames = 0;
        int n = 0;
        while (n < count && total < WINDOW) {
            float time = times[(head - 1 - n + times.length) % times.length];
            sorted[n++] = time;
            total += time;
            if (second < 1) {
                second += time;
                frames++;
            }
        }
        Arrays.sort(sorted, 0, n);
        int worst = Math.max(1, n / 100);
        float slow = 0;
        for (int i = n - worst; i < n; i++) slow += sorted[i];
        fps = Math.round(frames / second);
        low = Math.round(worst / slow);
    }

    int fps() {
        return fps;
    }

    int low() {
        return low;
    }
}
