package wtf.moonlight.ui.clickgui;

import wtf.moonlight.ui.imgui.Natives;

public final class Colors {
    private Colors() {
    }

    public static int rgba(int r, int g, int b, int a) {
        return r | g << 8 | b << 16 | a << 24;
    }

    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 255);
    }

    public static int red(int color) {
        return color & 0xFF;
    }

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int reAlpha(int color, int alpha) {
        return Natives.withAlpha(color, alpha);
    }

    public static int interpolate(int from, int to, float ratio) {
        return channel(from >>> 24, to >>> 24, ratio) << 24
                | channel(from >>> 16 & 0xFF, to >>> 16 & 0xFF, ratio) << 16
                | channel(from >>> 8 & 0xFF, to >>> 8 & 0xFF, ratio) << 8
                | channel(from & 0xFF, to & 0xFF, ratio);
    }

    public static int mix(int a, int b, float t) {
        float s = 1.0F / 255.0F;
        float amount = Math.clamp(t, 0.0F, 1.0F);
        int out = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            float from = (a >>> shift & 0xFF) * s;
            float to = (b >>> shift & 0xFF) * s;
            float value = from + (to - from) * amount;
            out |= (int) (Math.clamp(value, 0.0F, 1.0F) * 255.0F + 0.5F) << shift;
        }
        return out;
    }

    private static int channel(int a, int b, float ratio) {
        return (int) (a + (b - a) * ratio) & 0xFF;
    }
}
