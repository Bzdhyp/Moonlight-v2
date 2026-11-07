package wtf.moonlight.ui.hud;

public record Rect(float minX, float minY, float maxX, float maxY, float rounding) {
    boolean contains(float x, float y) {
        return x >= minX && x < maxX && y >= minY && y < maxY;
    }
}
