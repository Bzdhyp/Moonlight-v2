package wtf.moonlight.ui.imgui.post;

import com.mojang.renderpearl.api.commands.RenderPass;

public record Area(float minX, float minY, float maxX, float maxY) {
    public static final Area EMPTY = new Area(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

    public boolean isEmpty() {
        return maxX <= minX || maxY <= minY;
    }

    public Area union(Area other) {
        return new Area(Math.min(minX, other.minX), Math.min(minY, other.minY), Math.max(maxX, other.maxX), Math.max(maxY, other.maxY));
    }

    public Area intersect(Area other) {
        return new Area(Math.max(minX, other.minX), Math.max(minY, other.minY), Math.min(maxX, other.maxX), Math.min(maxY, other.maxY));
    }

    Area expand(float pad, int width, int height) {
        return new Area(minX - pad, minY - pad, maxX + pad, maxY + pad).intersect(new Area(0, 0, width, height));
    }

    boolean contains(Area other) {
        return other.minX >= minX && other.minY >= minY && other.maxX <= maxX && other.maxY <= maxY;
    }

    boolean scissor(RenderPass pass, int targetWidth, int targetHeight, int width, int height, int margin) {
        float scaleX = targetWidth / (float) width;
        float scaleY = targetHeight / (float) height;
        int x0 = Math.max(0, (int) Math.floor(minX * scaleX) - margin);
        int y0 = Math.max(0, (int) Math.floor(minY * scaleY) - margin);
        int x1 = Math.min(targetWidth, (int) Math.ceil(maxX * scaleX) + margin);
        int y1 = Math.min(targetHeight, (int) Math.ceil(maxY * scaleY) + margin);
        if (x1 <= x0 || y1 <= y0) return false;
        pass.enableScissor(x0, targetHeight - y1, x1 - x0, y1 - y0);
        return true;
    }
}
