package wtf.moonlight.ui.clickgui.element;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.bind.Bind;
import wtf.moonlight.bind.BindCapture;
import wtf.moonlight.bind.Hotkey;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.ModuleManager;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Theme;
import wtf.moonlight.ui.clickgui.Widgets;
import wtf.moonlight.utils.KeyUtils;

import java.util.List;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class BindElement extends Element {
    private static final float ICON_SIZE = 14;
    private static final float MENU = 168;
    private static final float SUBMENU = 184;
    private static final float LIST = 220;
    private static final float ROW = 30;
    private static final float PAD = 4;
    private static final float SPLIT = 9;
    private static final float SWITCH = 14;
    private static final int RED = Colors.rgb(239, 83, 80);
    private static final int KEYBOARD = 0xF11C;
    private static final int LIST_ICON = 0xF03A;
    private static final int PLUS = 0xF055;
    private static final int UNDO = 0xF0E2;
    private static final int KEY = 0xF084;
    private static final int LINK = 0xF0C1;
    private static final int EYE = 0xF06E;
    private static final int TRASH = 0xF2ED;
    private static final int TOGGLE = 0xF042;
    private static final int HOLD = 0xF256;

    private final Bind bind;
    private final BindCapture capture;
    private final ModuleManager modules;
    private @Nullable Hotkey editing;

    public BindElement(Panel panel, Bind bind, BindCapture capture, ModuleManager modules) {
        super(panel);
        this.bind = bind;
        this.capture = capture;
        this.modules = modules;
    }

    @Override
    public float width() {
        return scale(ICON_SIZE);
    }

    @Override
    public float height() {
        return scale(ICON_SIZE);
    }

    @Override
    public void draw() {
        Theme theme = panel.colors();
        if (ImGui.invisibleButton("bind", scale(ICON_SIZE), scale(ICON_SIZE))) ImGui.openPopup("##hotkeys");
        String glyph = Character.toString(KEYBOARD);
        Widgets.pushIcon();
        ImGui.getWindowDrawList().addText(ImGui.getItemRectMinX() + (scale(ICON_SIZE) - ImGui.calcTextSizeX(glyph)) / 2.0F,
                ImGui.getItemRectMinY() + (scale(ICON_SIZE) - ImGui.calcTextSizeY(glyph)) / 2.0F - scale(0.5f),
                ImGui.isPopupOpen("##hotkeys") ? theme.iconHighlight : bind.isBound() ? theme.main : theme.secondary, glyph);
        Widgets.popFont();
        List<Hotkey> hotkeys = bind.getHotkeys();
        ImGui.setNextWindowSize(scale(MENU), scale(PAD * 2 + ROW * (hotkeys.size() + 3) + SPLIT));
        if (!Widgets.beginPopup(theme, "##hotkeys")) {
            editing = null;
            return;
        }
        menu(theme, hotkeys);
        Widgets.endPopup();
    }

    private void menu(Theme theme, List<Hotkey> hotkeys) {
        float top = ImGui.getWindowPosY() + scale(PAD);
        float width = scale(MENU - PAD * 2);
        for (int i = 0; i < hotkeys.size(); i++) {
            Hotkey hotkey = hotkeys.get(i);
            float y = top + scale(ROW) * i;
            boolean open = hotkey == editing && ImGui.isPopupOpen("##hotkey");
            String label = capture.getTarget() == hotkey ? "Press a key..." : hotkey.label();
            if (row(theme, y, width, "hotkey" + i, KEYBOARD, label, false, open)) {
                editing = hotkey;
                ImGui.openPopup("##hotkey");
            }
            chevron(theme, y, width);
            if (hotkey == editing) submenu(theme, hotkey, y);
        }
        float y = top + scale(ROW) * hotkeys.size();
        boolean listing = ImGui.isPopupOpen("##all");
        if (row(theme, y, width, "all", LIST_ICON, "All Hotkeys", false, listing)) ImGui.openPopup("##all");
        chevron(theme, y, width);
        all(theme, y);
        y += scale(ROW);
        if (row(theme, y, width, "new", PLUS, "New Hotkey", false, false)) {
            Hotkey hotkey = bind.add(new Hotkey(KeyUtils.NONE));
            editing = hotkey;
            listen(hotkey);
            ImGui.openPopup("##hotkey");
        }
        y += scale(ROW);
        split(theme, y, width);
        if (row(theme, y + scale(SPLIT), width, "reset", UNDO, "Reset All", true, false)) {
            Hotkey target = capture.getTarget();
            if (target != null && bind.getHotkeys().contains(target)) capture.cancel();
            bind.clear();
            editing = null;
        }
    }

    private void submenu(Theme theme, Hotkey hotkey, float rowY) {
        ImGui.setNextWindowPos(ImGui.getWindowPosX() + scale(MENU + 6), rowY - scale(PAD), ImGuiCond.Always);
        ImGui.setNextWindowSize(scale(SUBMENU), scale(PAD * 2 + ROW * 5 + SPLIT));
        if (!Widgets.beginPopup(theme, "##hotkey")) return;
        ImDrawList draw = ImGui.getWindowDrawList();
        float top = ImGui.getWindowPosY() + scale(PAD);
        float width = scale(SUBMENU - PAD * 2);
        float right = ImGui.getWindowPosX() + scale(PAD) + width - scale(10);
        boolean listening = capture.getTarget() == hotkey;
        if (row(theme, top, width, "key", KEY, "Key", false, listening)) {
            if (listening) capture.cancel();
            else listen(hotkey);
        }
        badge(draw, theme, right, top, listening ? "..." : KeyUtils.getName(hotkey.getKey()));
        float y = top + scale(ROW);
        if (row(theme, y, width, "mode", KEYBOARD, "Mode", false, false)) hotkey.setHold(!hotkey.isHold());
        mode(draw, theme, right, y, Widgets.motion(ImGui.getID("##mode" + System.identityHashCode(hotkey)), hotkey.isHold() ? 1 : 0, SWITCH));
        y += scale(ROW);
        label(theme, y, width, "value", LINK, "Value");
        boolean value = Controls.toggle(draw, 1, right - scale(29), y + scale(ROW / 2 - 9), hotkey.getValue());
        if (value != hotkey.getValue()) hotkey.setValue(value);
        y += scale(ROW);
        label(theme, y, width, "visible", EYE, "Visible");
        boolean visible = Controls.toggle(draw, 2, right - scale(29), y + scale(ROW / 2 - 9), hotkey.isVisible());
        if (visible != hotkey.isVisible()) hotkey.setVisible(visible);
        y += scale(ROW);
        split(theme, y, width);
        if (row(theme, y + scale(SPLIT), width, "delete", TRASH, "Delete", true, false)) {
            if (listening) capture.cancel();
            bind.remove(hotkey);
            editing = null;
            ImGui.closeCurrentPopup();
        }
        Widgets.endPopup();
    }

    private void all(Theme theme, float rowY) {
        int count = 0;
        for (Module module : modules.getModules()) count += module.getBind().getHotkeys().size();
        ImGui.setNextWindowPos(ImGui.getWindowPosX() + scale(MENU + 6), rowY - scale(PAD), ImGuiCond.Always);
        ImGui.setNextWindowSize(scale(LIST), scale(PAD * 2 + ROW * Math.max(1, count)));
        if (!Widgets.beginPopup(theme, "##all")) return;
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = ImGui.getWindowPosX() + scale(PAD + 12);
        float right = ImGui.getWindowPosX() + scale(LIST - PAD - 12);
        float y = ImGui.getWindowPosY() + scale(PAD);
        Widgets.pushText(Widgets.TEXT_CONTROL);
        float offset = (scale(ROW) - Widgets.textHeight()) * .5f;
        if (count == 0) draw.addText(x, y + offset, theme.secondary, "No hotkeys");
        for (Module module : modules.getModules()) {
            for (Hotkey hotkey : module.getBind().getHotkeys()) {
                String label = hotkey.label();
                draw.addText(x, y + offset, theme.main, module.getName());
                draw.addText(right - Widgets.textWidth(label), y + offset, theme.secondary, label);
                y += scale(ROW);
            }
        }
        Widgets.popFont();
        Widgets.endPopup();
    }

    private void listen(Hotkey hotkey) {
        capture.begin(hotkey, () -> {
        }, () -> {
            if (hotkey.getKey() == KeyUtils.NONE) {
                bind.remove(hotkey);
                if (editing == hotkey) editing = null;
            }
        });
    }

    private boolean row(Theme theme, float y, float width, String id, int icon, String label, boolean danger, boolean lit) {
        float x = ImGui.getWindowPosX() + scale(PAD);
        ImGui.setCursorScreenPos(x, y);
        boolean clicked = ImGui.invisibleButton(id, width, scale(ROW));
        highlight(theme, x, y, width, Widgets.motion(ImGui.getItemID() ^ 0x3131, lit ? 1 : ImGui.isItemHovered() ? .7f : 0, 22));
        text(x, y, icon, label, danger ? RED : theme.secondary, danger ? RED : theme.main);
        return clicked;
    }

    private void label(Theme theme, float y, float width, String id, int icon, String label) {
        float x = ImGui.getWindowPosX() + scale(PAD);
        boolean hovered = ImGui.isWindowHovered() && ImGui.isMouseHoveringRect(x, y, x + width, y + scale(ROW));
        highlight(theme, x, y, width, Widgets.motion(ImGui.getID(id) ^ 0x3131, hovered ? .7f : 0, 22));
        text(x, y, icon, label, theme.secondary, theme.main);
    }

    private static void highlight(Theme theme, float x, float y, float width, float hover) {
        ImGui.getWindowDrawList().addRectFilled(x, y, x + width, y + scale(ROW),
                Colors.reAlpha(theme.selectorElementHover, (int) (Colors.alpha(theme.selectorElementHover) * hover)), scale(8));
    }

    private static void text(float x, float y, int icon, String label, int iconColor, int color) {
        ImDrawList draw = ImGui.getWindowDrawList();
        float middle = y + scale(ROW) * .5f;
        Widgets.pushIcon();
        draw.addText(x + scale(12), middle - Widgets.textHeight() * .5f, iconColor, Character.toString(icon));
        Widgets.popFont();
        Widgets.pushText(Widgets.TEXT_CONTROL);
        draw.addText(x + scale(34), middle - Widgets.textHeight() * .5f, color, label);
        Widgets.popFont();
    }

    private static void chevron(Theme theme, float y, float width) {
        float right = ImGui.getWindowPosX() + scale(PAD) + width;
        Controls.chevron(ImGui.getWindowDrawList(), right - scale(16), y + scale(ROW / 2 - 3), theme.secondary);
    }

    private static void split(Theme theme, float y, float width) {
        float x = ImGui.getWindowPosX() + scale(PAD);
        float middle = y + scale(SPLIT) * .5f;
        ImGui.getWindowDrawList().addLine(x + scale(10), middle, x + width - scale(10), middle, theme.split, scale(1));
    }

    private static void badge(ImDrawList draw, Theme theme, float right, float y, String key) {
        Widgets.pushText(Widgets.TEXT_SMALL);
        float width = Math.max(scale(20), Widgets.textWidth(key) + scale(12));
        float top = y + scale(ROW / 2 - 10);
        draw.addRectFilled(right - width, top, right, top + scale(20), theme.selectorBg, scale(5));
        draw.addText(right - width + (width - Widgets.textWidth(key)) * .5f, top + (scale(20) - Widgets.textHeight()) * .5f, theme.main, key);
        Widgets.popFont();
    }

    private static void mode(ImDrawList draw, Theme theme, float right, float y, float hold) {
        float toggle = pill(TOGGLE, "Toggle");
        float width = toggle + (pill(HOLD, "Hold") - toggle) * hold;
        float top = y + scale(ROW / 2 - 10);
        float bottom = top + scale(20);
        draw.addRectFilled(right - width, top, right, bottom, Colors.reAlpha(theme.iconHighlight, 40), scale(10));
        draw.pushClipRect(right - width, top, right, bottom, true);
        content(draw, theme, right - width, top - scale(20) * hold, TOGGLE, "Toggle", (1 - hold) * (1 - hold));
        content(draw, theme, right - width, top + scale(20) * (1 - hold), HOLD, "Hold", hold * hold);
        draw.popClipRect();
    }

    private static float pill(int icon, String label) {
        Widgets.pushIcon();
        float iconWidth = Widgets.textWidth(Character.toString(icon));
        Widgets.popFont();
        Widgets.pushText(Widgets.TEXT_SMALL);
        float textWidth = Widgets.textWidth(label);
        Widgets.popFont();
        return scale(8) + iconWidth + scale(5) + textWidth + scale(9);
    }

    private static void content(ImDrawList draw, Theme theme, float x, float top, int icon, String label, float alpha) {
        if (alpha <= 0.01F) return;
        int color = Colors.reAlpha(theme.iconHighlight, Math.round(Colors.alpha(theme.iconHighlight) * alpha));
        String glyph = Character.toString(icon);
        Widgets.pushIcon();
        float iconWidth = Widgets.textWidth(glyph);
        draw.addText(x + scale(8), top + (scale(20) - Widgets.textHeight()) * .5f, color, glyph);
        Widgets.popFont();
        Widgets.pushText(Widgets.TEXT_SMALL);
        draw.addText(x + scale(8) + iconWidth + scale(5), top + (scale(20) - Widgets.textHeight()) * .5f, color, label);
        Widgets.popFont();
    }
}
