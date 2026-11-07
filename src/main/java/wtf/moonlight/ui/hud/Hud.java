package wtf.moonlight.ui.hud;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.input.CharInputEvent;
import wtf.moonlight.event.impl.input.KeyInputEvent;
import wtf.moonlight.event.impl.input.MouseInputEvent;
import wtf.moonlight.event.impl.input.MouseMoveEvent;
import wtf.moonlight.ui.clickgui.ClientSettings;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.imgui.ImGuiManager;
import wtf.moonlight.ui.imgui.Input;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class Hud {
    private static final int OUTLINE = Colors.rgb(255, 255, 255);
    private static final int LIGHT_OUTLINE = Colors.rgb(28, 30, 38);
    private static final float SNAP = 6;
    private static final float GUIDE_FADE = 18;
    private static final int GUIDE_ALPHA = 200;

    private final List<HudElement> elements;
    private final Panel panel;
    private final ClientSettings settings;
    private final BooleanSupplier chat;
    private @Nullable HudElement hovered;
    private @Nullable HudElement dragged;
    private @Nullable HudElement owner;
    private float grabX;
    private float grabY;
    private float lineX;
    private float lineY;
    private boolean snapX;
    private boolean snapY;
    private boolean capture;
    private boolean chatting;
    private int held;

    public Hud(List<HudElement> elements, Panel panel, ClientSettings settings, BooleanSupplier chat) {
        this.elements = elements;
        this.panel = panel;
        this.settings = settings;
        this.chat = chat;
    }

    public void frame(boolean editing, boolean visible) {
        boolean inChat = chat.getAsBoolean();
        if (inChat != chatting) {
            chatting = inChat;
            if (inChat) {
                Input.focus(true);
                Input.mouseMoved();
            } else if (!editing) {
                Input.focus(false);
                Input.mouseLeft();
            }
        }
        editing &= visible;
        float mouseX = ImGui.getMousePosX();
        float mouseY = ImGui.getMousePosY();
        boolean overWindow = ImGui.isWindowHovered(ImGuiHoveredFlags.AnyWindow);
        if (!editing || !ImGui.isMouseDown(ImGuiMouseButton.Left)) dragged = null;
        hovered = null;
        if (editing && dragged == null && !overWindow) {
            for (HudElement element : elements) {
                Rect bounds = element.bounds();
                if (bounds != null && bounds.contains(mouseX, mouseY)) hovered = element;
            }
        }
        if (!editing) owner = null;
        HudElement target = hovered;
        Rect hit = target != null ? target.bounds() : null;
        if (target != null && hit != null) {
            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                dragged = target;
                grabX = mouseX - hit.minX();
                grabY = mouseY - hit.minY();
            }
            if (ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
                panel.settings(target.module());
                owner = target;
            }
        }
        HudElement moving = dragged;
        snapX = false;
        snapY = false;
        if (moving != null) drag(moving, mouseX - grabX, mouseY - grabY);
        capture = editing && (moving != null || hovered != null || overWindow);
        if (moving != null || hovered != null) ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll);
        if (!visible) return;

        ImDrawList draw = ImGui.getBackgroundDrawList();
        boolean light = settings.isLight();
        Style style = new Style(settings.hudScale(), light, settings.accent(), settings.hudOpacity());
        for (HudElement element : elements) {
            element.frame(style);
            Rect bounds = element.bounds();
            float glow = Controls.motion(System.identityHashCode(element), element == hovered || element == moving ? 1 : 0, 16, 0);
            if (bounds == null || glow < 0.01F) continue;
            draw.addRect(bounds.minX(), bounds.minY(), bounds.maxX(), bounds.maxY(),
                    Colors.reAlpha(light ? LIGHT_OUTLINE : OUTLINE, (int) (110 * glow)), bounds.rounding(), 0, ImGuiManager.displayScale());
        }
        guides(draw, settings.accent());
        HudElement anchor = owner;
        Rect anchored = anchor != null ? anchor.bounds() : null;
        if (anchored != null) panel.follow(anchored.minX(), anchored.minY(), anchored.maxX(), anchored.maxY());
    }

    private void drag(HudElement moving, float left, float top) {
        Rect bounds = moving.bounds();
        if (bounds == null) {
            moving.place(left, top);
            return;
        }
        ImGuiIO io = ImGui.getIO();
        float width = bounds.maxX() - bounds.minX();
        float height = bounds.maxY() - bounds.minY();
        float range = SNAP * ImGuiManager.displayScale();
        float[] xs = {io.getDisplaySizeX() / 2};
        float[] ys = {io.getDisplaySizeY() / 2};
        for (HudElement element : elements) {
            Rect other = element.bounds();
            if (element == moving || other == null) continue;
            xs = append(xs, other.minX(), (other.minX() + other.maxX()) / 2, other.maxX());
            ys = append(ys, other.minY(), (other.minY() + other.maxY()) / 2, other.maxY());
        }
        float shiftX = Float.NaN;
        for (float guide : xs) {
            for (float edge : new float[]{left, left + width / 2, left + width}) {
                float shift = guide - edge;
                if (Math.abs(shift) <= range && (Float.isNaN(shiftX) || Math.abs(shift) < Math.abs(shiftX))) {
                    shiftX = shift;
                    lineX = guide;
                }
            }
        }
        float shiftY = Float.NaN;
        for (float guide : ys) {
            for (float edge : new float[]{top, top + height / 2, top + height}) {
                float shift = guide - edge;
                if (Math.abs(shift) <= range && (Float.isNaN(shiftY) || Math.abs(shift) < Math.abs(shiftY))) {
                    shiftY = shift;
                    lineY = guide;
                }
            }
        }
        snapX = !Float.isNaN(shiftX);
        snapY = !Float.isNaN(shiftY);
        moving.place(snapX ? left + shiftX : left, snapY ? top + shiftY : top);
    }

    private void guides(ImDrawList draw, int accent) {
        ImGuiIO io = ImGui.getIO();
        float thickness = ImGuiManager.displayScale();
        float alphaX = Controls.motion(System.identityHashCode(this) ^ 0x6A1D, snapX ? 1 : 0, GUIDE_FADE, 0);
        float alphaY = Controls.motion(System.identityHashCode(this) ^ 0x6A1E, snapY ? 1 : 0, GUIDE_FADE, 0);
        if (alphaX > 0.01F) draw.addLine(lineX, 0, lineX, io.getDisplaySizeY(), Colors.reAlpha(accent, (int) (GUIDE_ALPHA * alphaX)), thickness);
        if (alphaY > 0.01F) draw.addLine(0, lineY, io.getDisplaySizeX(), lineY, Colors.reAlpha(accent, (int) (GUIDE_ALPHA * alphaY)), thickness);
    }

    private static float[] append(float[] values, float... more) {
        float[] out = Arrays.copyOf(values, values.length + more.length);
        System.arraycopy(more, 0, out, values.length, more.length);
        return out;
    }

    @Subscribe
    private void onMouseMove(MouseMoveEvent event) {
        if (chat.getAsBoolean()) Input.mouseMoved();
    }

    @Subscribe
    private void onMouse(MouseInputEvent event) {
        if (!chat.getAsBoolean()) return;
        int bit = 1 << event.getButton();
        Input.mouseMoved();
        Input.mouseButton(event.getButton(), event.isPress());
        if (event.isPress()) {
            if (!capture) return;
            held |= bit;
            event.cancel();
        } else if ((held & bit) != 0) {
            held &= ~bit;
            event.cancel();
        }
    }

    @Subscribe
    private void onKey(KeyInputEvent event) {
        if (!chat.getAsBoolean() || !Input.wantsText()) return;
        Input.key(event.getEvent(), event.isPress() || event.isRepeat());
        event.cancel();
    }

    @Subscribe
    private void onChar(CharInputEvent event) {
        if (!chat.getAsBoolean() || !Input.wantsText()) return;
        Input.character(event.getCodepoint());
        event.cancel();
    }
}
