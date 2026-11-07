package wtf.moonlight.ui.clickgui;

import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;

final class Placement {
    private float fractionX;
    private float fractionY;
    private boolean known;
    private boolean placed;
    private boolean tracked;
    private boolean moved;
    private boolean centered;
    private float width;
    private float height;
    private float lastX;
    private float lastY;
    private float appliedWidth = -1;
    private float appliedHeight = -1;

    void placeCentered(float width, float height) {
        float displayX = ImGui.getIO().getDisplaySizeX();
        float displayY = ImGui.getIO().getDisplaySizeY();
        reflow(displayX, displayY);
        boolean resized = this.width != width || this.height != height;
        centered = true;
        this.width = width;
        this.height = height;
        if (!known || (!moved && (resized || !placed))) {
            center(displayX, displayY);
            placed = false;
        }
        if (resized && moved && displayX > 0 && displayY > 0) {
            float maxX = displayX > width ? (displayX - width) / displayX : 0;
            float maxY = displayY > height ? (displayY - height) / displayY : 0;
            fractionX = Math.min(fractionX, maxX);
            fractionY = Math.min(fractionY, maxY);
            placed = false;
        }
        if (placed || !known || displayX <= 0 || displayY <= 0) return;
        placed = true;
        tracked = false;
        ImGui.setNextWindowPos(fractionX * displayX, fractionY * displayY);
    }

    void capture() {
        float displayX = ImGui.getIO().getDisplaySizeX();
        float displayY = ImGui.getIO().getDisplaySizeY();
        if (displayX <= 0 || displayY <= 0) return;
        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        if (!tracked) {
            tracked = true;
            lastX = x;
            lastY = y;
            return;
        }
        if (Math.abs(x - lastX) < 0.5f && Math.abs(y - lastY) < 0.5f) return;
        lastX = x;
        lastY = y;
        if (!ImGui.isMouseDown(ImGuiMouseButton.Left)) return;
        fractionX = clamp01(x / displayX);
        fractionY = clamp01(y / displayY);
        moved = true;
        known = true;
    }

    private void reflow(float displayX, float displayY) {
        if (displayX <= 0 || displayY <= 0) return;
        if (displayX == appliedWidth && displayY == appliedHeight) return;
        appliedWidth = displayX;
        appliedHeight = displayY;
        if (centered && !moved) center(displayX, displayY);
        placed = false;
    }

    private void center(float displayX, float displayY) {
        if (displayX <= 0 || displayY <= 0) return;
        fractionX = clamp01((displayX - width) * 0.5f / displayX);
        fractionY = clamp01((displayY - height) * 0.5f / displayY);
        known = true;
    }

    private static float clamp01(float value) {
        return value < 0 ? 0 : Math.min(value, 1);
    }
}
