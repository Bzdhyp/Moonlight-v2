package wtf.moonlight.ui.clickgui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import wtf.moonlight.config.ConfigManager;
import wtf.moonlight.ui.imgui.Fonts;
import wtf.moonlight.ui.imgui.Natives;

import java.util.List;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

final class TopBar {
    private static final float BAR_HEIGHT = 56;
    private static final float COLUMN_GAP = 11;
    private static final float WORDMARK_SIZE = 28;
    private static final float WORDMARK_TRACKING = 1.2f;
    private static final float WORDMARK_SHIFT = 1.5f;
    private static final float WORDMARK_CYCLE = 2;
    private static final float SELECTOR_HEIGHT = 30;
    private static final float SELECTOR_ROUNDING = 6;
    private static final float SEARCH_BUTTON = 28;
    private static final float SEARCH_FIELD = 150;
    private static final float SEARCH_GAP = 6;
    private static final float POPUP_WIDTH = 268;
    private static final float POPUP_ROW = 32;
    private static final float POPUP_ROUNDING = 10;
    private static final float POPUP_ACTION = 32;
    private static final float POPUP_INSET = 8;
    private static final float POPUP_GAP = 6;
    private static final float POPUP_LIST_HEIGHT = 168;
    private static final double STATUS_SECONDS = 3.0;
    private static final int LOGO_MAIN = 0xFFE5D2CF;
    private static final int LOGO_FIRST = 0xFFFF7D66;
    private static final int LOGO_SECOND = 0xFF663329;
    private static final int DANGER = Colors.rgb(0xF0, 0x6A, 0x6A);
    private static final String SAVE_ICON = Character.toString(0xF0C7);
    private static final String SEARCH_HINT = "search modules";

    private final Panel panel;
    private final ImString nameBuffer = new ImString(63);
    private final ImString createBuffer = new ImString(63);
    private final ImString searchBuffer = new ImString(63);
    private int searchFrame = -2;
    private float searchReveal;
    private float searchHighlight;
    private float searchFocus;
    private boolean searchOpen;
    private boolean focusSearch;
    private int renaming = -1;
    private boolean focusEdit;
    private float barX;
    private float barY;
    private float barCenterY;
    private int seenStatus;
    private double statusAt = -1000.0;

    TopBar(Panel panel) {
        this.panel = panel;
    }

    void render() {
        Windows.beginChild("topbar", 0, scale(BAR_HEIGHT), ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoBackground);
        barX = ImGui.getCursorScreenPosX();
        barY = ImGui.getCursorScreenPosY();
        barCenterY = barY + scale(BAR_HEIGHT) / 2.0F;
        Theme theme = panel.colors();
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(barX + scale(158), barY, barX + scale(748), barY + scale(56),
                theme.isDark() ? Colors.rgba(11, 13, 20, 225) : Colors.rgba(240, 243, 249, 225), scale(14), ImDrawFlags.RoundCornersTopRight);
        draw.addLine(barX + scale(158), barY + scale(56), barX + scale(748), barY + scale(56), theme.split, scale(1));
        renderTitle(theme);
        renderSelector(theme);
        renderSearch(theme);
        renderSelectorPopup(theme);
        Windows.endChild();
    }

    private void renderTitle(Theme theme) {
        ImDrawList draw = ImGui.getWindowDrawList();
        ImGui.pushFont(Fonts.wordmark(), WORDMARK_SIZE);
        String name = panel.settings().clientName();
        float naturalWidth = wordmarkWidth(name);
        if (naturalWidth > scale(138)) {
            Widgets.popFont();
            ImGui.pushFont(Fonts.wordmark(), WORDMARK_SIZE * scale(138) / naturalWidth);
        }
        float width = wordmarkWidth(name);
        draw.pushClipRect(barX + scale(8), barY, barX + scale(150), barY + scale(56), true);
        drawTitle(draw, name, barX + (scale(158) - width) / 2.0F, barCenterY - Widgets.textHeight() / 2.0F,
                ((float) ImGui.getTime() / WORDMARK_CYCLE) % 1.0F, scale(WORDMARK_SHIFT));
        draw.popClipRect();
        Widgets.popFont();
        draw.addLine(barX + scale(10), barY + scale(56), barX + scale(147), barY + scale(56), theme.split, scale(1));
    }

    private void renderSelector(Theme theme) {
        ConfigManager configs = panel.configs();
        String current = configs.getCurrent();
        float openWidth = scale(136);
        float minX = barX + scale(158) + scale(COLUMN_GAP);
        float minY = barCenterY - scale(SELECTOR_HEIGHT) / 2.0F;
        float splitX = minX + scale(SELECTOR_HEIGHT);
        float maxX = splitX + openWidth;
        float maxY = minY + scale(SELECTOR_HEIGHT);

        ImGui.setCursorScreenPos(minX, minY);
        ImGui.invisibleButton("configsave", scale(SELECTOR_HEIGHT), scale(SELECTOR_HEIGHT));
        boolean saveHovered = ImGui.isItemHovered();
        if (ImGui.isItemClicked()) {
            configs.saveCurrent();
            statusAt = ImGui.getTime();
        }
        ImGui.setCursorScreenPos(splitX, minY);
        ImGui.invisibleButton("configopen", openWidth, scale(SELECTOR_HEIGHT));
        boolean openHovered = ImGui.isItemHovered();
        boolean openClicked = ImGui.isItemClicked();

        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(minX, minY, maxX, maxY, Colors.reAlpha(theme.modulePanelBg, 255), scale(SELECTOR_ROUNDING));
        if (saveHovered) draw.addRectFilled(minX, minY, splitX, maxY, theme.buttonHoveredBg, scale(SELECTOR_ROUNDING), ImDrawFlags.RoundCornersLeft);
        if (openHovered) draw.addRectFilled(splitX, minY, maxX, maxY, theme.buttonHoveredBg, scale(SELECTOR_ROUNDING), ImDrawFlags.RoundCornersRight);
        Widgets.pushIcon();
        draw.addText(minX + scale(19) - Widgets.textWidth(SAVE_ICON) / 2.0F, barCenterY - Widgets.textHeight() / 2.0F,
                saveHovered ? theme.iconHighlight : theme.main, SAVE_ICON);
        Widgets.popFont();
        Widgets.pushText(Widgets.TEXT_CONTROL);
        draw.pushClipRect(minX + scale(48), minY, maxX - scale(22), maxY, true);
        draw.addText(minX + scale(48), barCenterY - Widgets.textHeight() / 2, theme.main, current);
        draw.popClipRect();
        Widgets.popFont();
        Controls.chevron(draw, minX + scale(148), minY + scale(11), theme.secondary);
        draw.addRect(minX, minY, maxX, maxY, theme.split, scale(SELECTOR_ROUNDING), 0, scale(1));
        if (openClicked) {
            ImGui.setNextWindowPos(minX, maxY + scale(6), ImGuiCond.Appearing);
            ImGui.openPopup("configpopup");
        }
    }

    private void renderSelectorPopup(Theme theme) {
        ImGui.pushStyleVar(ImGuiStyleVar.PopupRounding, scale(12));
        ImGui.pushStyleVar(ImGuiStyleVar.PopupBorderSize, scale(1));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, scale(POPUP_INSET), scale(POPUP_INSET));
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, scale(POPUP_GAP));
        ImGui.pushStyleColor(ImGuiCol.PopupBg, theme.modulePanelBg);
        ImGui.pushStyleColor(ImGuiCol.Border, theme.windowBorder);
        ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
        ImGui.pushStyleColor(ImGuiCol.TextDisabled, theme.secondary);
        ImGui.pushStyleColor(ImGuiCol.Header, theme.selectorElementActive);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, theme.selectorElementHover);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, theme.selectorElementActive);
        if (Widgets.beginPopup(theme, "configpopup")) {
            renderConfigList(theme);
            Widgets.endPopup();
        } else if (renaming >= 0) {
            cancelEdit();
        }
        ImGui.popStyleColor(7);
        ImGui.popStyleVar(4);
    }

    private void renderConfigList(Theme theme) {
        ConfigManager configs = panel.configs();
        Action action = new Action();
        renderCreateRow(theme);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, scale(2));
        Windows.beginChild("configlist", scale(POPUP_WIDTH), scale(POPUP_LIST_HEIGHT), ImGuiWindowFlags.NoBackground);
        List<String> names = configs.getConfigs();
        if (names.isEmpty() && renaming < 0) {
            Widgets.pushText(Widgets.TEXT_SMALL);
            ImGui.pushStyleColor(ImGuiCol.Text, theme.secondary);
            ImGui.textUnformatted("No configs yet");
            ImGui.popStyleColor();
            Widgets.popFont();
        }
        for (int index = 0; index < names.size(); index++) {
            if (renaming == index) {
                renderEditRow();
                continue;
            }
            renderConfigRow(theme, names, index, action);
        }
        if (ImGui.isWindowHovered() && !ImGui.isAnyItemHovered() && ImGui.isMouseClicked(1)) ImGui.openPopup("listmenu");
        if (Widgets.beginPopup(theme, "listmenu")) {
            if (Widgets.choice(theme, "refresh", "Refresh", false, scale(POPUP_WIDTH))) action.refresh = true;
            if (Widgets.choice(theme, "folder", "Open folder", false, scale(POPUP_WIDTH))) action.openFolder = true;
            Widgets.endPopup();
        }
        Windows.endChild();
        ImGui.popStyleVar();
        renderStatus(theme);

        if (action.overwrite) {
            configs.saveAs(action.target);
            statusAt = ImGui.getTime();
        }
        if (action.duplicate) {
            configs.duplicate(action.target);
            statusAt = ImGui.getTime();
        }
        if (action.remove) {
            configs.delete(action.target);
            statusAt = ImGui.getTime();
        }
        if (action.refresh) {
            configs.refresh();
            statusAt = ImGui.getTime();
        }
        if (action.openFolder) configs.openFolder();
        if (action.renameIndex >= 0) beginRename(action.renameIndex);
    }

    private void renderConfigRow(Theme theme, List<String> names, int index, Action action) {
        ConfigManager configs = panel.configs();
        String name = names.get(index);
        boolean selected = name.equals(configs.getCurrent());
        ImGui.pushID(index);
        float minX = ImGui.getCursorScreenPosX();
        float minY = ImGui.getCursorScreenPosY();
        float maxX = minX + scale(POPUP_WIDTH);
        float maxY = minY + scale(POPUP_ROW);
        ImGui.setNextItemAllowOverlap();
        ImGui.invisibleButton("row", scale(POPUP_WIDTH), scale(POPUP_ROW));
        boolean hovered = ImGui.isItemHovered();
        boolean clicked = ImGui.isItemClicked();
        ImDrawList draw = ImGui.getWindowDrawList();
        float hover = Widgets.motion(ImGui.getID("##rowhover"), hovered ? 1 : 0, 22);
        draw.addRectFilled(minX, minY, maxX, maxY,
                Colors.reAlpha(theme.selectorElementHover, (int) (Colors.alpha(theme.selectorElementHover) * hover)), scale(10));
        if (selected) Controls.check(draw, minX + scale(14), minY + scale(16), theme.textHighlight);
        Widgets.pushText(Widgets.TEXT_CONTROL);
        draw.pushClipRect(minX, minY, maxX - scale(32), maxY, true);
        draw.addText(minX + scale(35), minY + (scale(32) - Widgets.textHeight()) * .5f, selected ? theme.textHighlight : theme.main, name);
        draw.popClipRect();
        Widgets.popFont();

        float dotsX = maxX - scale(POPUP_ACTION) / 2.0F - scale(4);
        ImGui.setCursorScreenPos(dotsX - scale(POPUP_ACTION) / 2.0F, minY);
        ImGui.invisibleButton("rowactions", scale(POPUP_ACTION), scale(POPUP_ROW));
        boolean dotsHovered = ImGui.isItemHovered();
        if (ImGui.isItemClicked()) ImGui.openPopup("rowmenu");
        dots(draw, dotsX, minY + scale(POPUP_ROW) / 2.0F, scale(15), dotsHovered ? theme.textHighlight : theme.secondary);

        if (Widgets.beginPopup(theme, "rowmenu")) {
            if (actionItem("Overwrite", TopBar::file, theme.main, theme.secondary, theme.selectorElementHover)) {
                action.overwrite = true;
                action.target = name;
            }
            if (actionItem("Rename", TopBar::pencil, theme.main, theme.secondary, theme.selectorElementHover)) action.renameIndex = index;
            if (actionItem("Duplicate", TopBar::copy, theme.main, theme.secondary, theme.selectorElementHover)) {
                action.duplicate = true;
                action.target = name;
            }
            if (actionItem("Open folder", TopBar::folder, theme.main, theme.secondary, theme.selectorElementHover)) action.openFolder = true;
            if (actionItem("Delete", TopBar::trash, DANGER, DANGER, theme.selectorElementHover)) {
                action.remove = true;
                action.target = name;
            }
            Widgets.endPopup();
        }
        ImGui.popID();
        if (clicked && !selected && ImGui.getIO().getMousePosX() < dotsX - scale(POPUP_ACTION)) {
            configs.load(name);
            ImGui.closeCurrentPopup();
        }
    }

    private void renderCreateRow(Theme theme) {
        float fieldWidth = scale(POPUP_WIDTH) - scale(POPUP_ACTION) - scale(POPUP_GAP);
        boolean entered = configInput(theme, "##newconfig", "Config Name", createBuffer, fieldWidth);
        float fieldHeight = ImGui.getItemRectSizeY();
        ImGui.sameLine(0, scale(POPUP_GAP));
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        ImGui.invisibleButton("createconfig", scale(POPUP_ACTION), fieldHeight);
        boolean hovered = ImGui.isItemHovered();
        boolean pressed = ImGui.isItemClicked();
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(x, y, x + scale(POPUP_ACTION), y + fieldHeight, hovered ? theme.buttonHoveredBg : theme.selectorBg, scale(POPUP_ROUNDING));
        plus(draw, x + scale(POPUP_ACTION) / 2.0F, y + fieldHeight / 2.0F, scale(14), hovered ? theme.iconHighlight : theme.textHighlight);
        if (entered || pressed) {
            String wanted = ConfigManager.sanitize(createBuffer.get());
            if (!wanted.isEmpty() && panel.configs().saveAs(wanted)) {
                createBuffer.set("");
                statusAt = ImGui.getTime();
            }
        }
    }

    private void renderEditRow() {
        if (focusEdit) {
            ImGui.setKeyboardFocusHere();
            focusEdit = false;
        }
        boolean entered = configInput(panel.colors(), "##configname", "config name", nameBuffer, scale(POPUP_WIDTH));
        if (entered) commitRename();
        else if (ImGui.isItemDeactivated()) cancelEdit();
    }

    private void renderStatus(Theme theme) {
        ConfigManager configs = panel.configs();
        if (configs.getStatusSerial() != seenStatus) {
            seenStatus = configs.getStatusSerial();
            statusAt = ImGui.getTime();
        }
        if (configs.getStatus().isEmpty() || ImGui.getTime() - statusAt >= STATUS_SECONDS) return;
        Widgets.pushText(Widgets.TEXT_SMALL);
        ImGui.pushStyleColor(ImGuiCol.Text, theme.secondary);
        ImGui.textUnformatted(configs.getStatus());
        ImGui.popStyleColor();
        Widgets.popFont();
    }

    private boolean configInput(Theme theme, String id, String hint, ImString buffer, float width) {
        Widgets.pushText(Widgets.TEXT_BODY);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, scale(POPUP_ROUNDING));
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, scale(8), scale(6));
        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 0);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, theme.selectorBg);
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, theme.selectorBg);
        ImGui.pushStyleColor(ImGuiCol.FrameBgActive, theme.selectorBg);
        ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
        ImGui.pushStyleColor(ImGuiCol.TextDisabled, theme.secondary);
        ImGui.pushStyleColor(ImGuiCol.TextSelectedBg, Colors.reAlpha(theme.iconHighlight, Controls.FIELD_SELECTION_ALPHA));
        ImGui.pushStyleColor(ImGuiCol.NavCursor, 0);
        ImGui.setNextItemWidth(width);
        boolean entered = ImGui.inputTextWithHint(id, hint, buffer, ImGuiInputTextFlags.EnterReturnsTrue);
        ImGui.popStyleColor(7);
        ImGui.popStyleVar(3);
        Widgets.popFont();
        return entered;
    }

    private void beginRename(int index) {
        List<String> names = panel.configs().getConfigs();
        if (index < 0 || index >= names.size()) return;
        renaming = index;
        nameBuffer.set(names.get(index));
        focusEdit = true;
    }

    private void commitRename() {
        List<String> names = panel.configs().getConfigs();
        if (renaming >= 0 && renaming < names.size()) {
            String wanted = ConfigManager.sanitize(nameBuffer.get());
            if (!wanted.isEmpty() && panel.configs().rename(names.get(renaming), wanted)) statusAt = ImGui.getTime();
        }
        cancelEdit();
    }

    private void cancelEdit() {
        renaming = -1;
        focusEdit = false;
        nameBuffer.set("");
    }

    private void renderSearch(Theme theme) {
        int frame = ImGui.getFrameCount();
        float delta = ImGui.getIO().getDeltaTime();
        boolean open = panel.isSearching();
        if (searchFrame != frame - 1) {
            searchReveal = open ? 1 : 0;
            searchHighlight = open ? 1 : 0;
            searchFocus = 0;
            searchOpen = open;
            focusSearch = open;
        }
        searchFrame = frame;
        if (!panel.getSearch().equals(searchBuffer.get())) searchBuffer.set(panel.getSearch());

        float buttonX = barX + scale(705);
        float buttonY = barCenterY - scale(SEARCH_BUTTON) / 2.0F;
        ImGui.setCursorScreenPos(buttonX, buttonY);
        ImGui.invisibleButton("searchtoggle", scale(SEARCH_BUTTON), scale(SEARCH_BUTTON));
        boolean hovered = ImGui.isItemHovered();
        if (ImGui.isItemClicked()) {
            open = !open;
            panel.setSearching(open);
            if (!open) searchBuffer.set("");
        }
        if (open != searchOpen) focusSearch = open;
        searchOpen = open;
        float target = open ? 1 : 0;
        float blend = 1.0F - (float) Math.exp(-20.0F * delta);
        searchReveal = searchReveal + blend * (target - searchReveal);
        if (Math.abs(searchReveal - target) < .0005f) searchReveal = target;
        float reveal = Controls.popupEase(searchReveal);
        searchHighlight = searchHighlight + (1.0F - (float) Math.exp(-16.0F * delta)) * ((open || hovered ? 1 : 0) - searchHighlight);
        magnifier(ImGui.getWindowDrawList(), buttonX + scale(SEARCH_BUTTON) / 2.0F, buttonY + scale(SEARCH_BUTTON) / 2.0F, scale(14),
                Colors.mix(theme.secondary, theme.iconHighlight, searchHighlight * .35f));

        float width = scale(SEARCH_FIELD) * reveal;
        if (width < scale(1)) {
            if (!open) searchBuffer.set("");
            return;
        }
        Widgets.pushText(Widgets.TEXT_CONTROL);
        float height = scale(SELECTOR_HEIGHT);
        float right = buttonX - scale(SEARCH_GAP);
        float minX = right - width;
        float minY = barCenterY - height / 2.0F;
        float maxY = minY + height;
        float rounding = Math.min(scale(SELECTOR_ROUNDING), width * 0.5f);
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilled(minX, minY, right, maxY, Colors.reAlpha(theme.modulePanelBg, 255), rounding);
        draw.addRectFilled(minX, minY, right, maxY,
                Colors.reAlpha(theme.buttonHoveredBg, (int) (Colors.alpha(theme.buttonHoveredBg) * searchFocus)), rounding);
        ImGui.pushClipRect(minX, minY, right, maxY, true);
        float[] ink = ink(searchBuffer.get().isEmpty() ? SEARCH_HINT : searchBuffer.get());
        float paddingY = Math.max(0, (height - Widgets.textHeight()) * .5f);
        float inkOffset = ink[1] > ink[0] ? (height - ink[0] - ink[1]) * .5f - paddingY : 0;
        ImGui.setCursorScreenPos(right - scale(SEARCH_FIELD), (float) Math.floor(minY + inkOffset + 0.5f));
        ImGui.setNextItemWidth(scale(SEARCH_FIELD));
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, scale(SELECTOR_ROUNDING));
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, scale(8), paddingY);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.DisabledAlpha, 1);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0);
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, 0);
        ImGui.pushStyleColor(ImGuiCol.FrameBgActive, 0);
        ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
        ImGui.pushStyleColor(ImGuiCol.TextDisabled, theme.secondary);
        ImGui.pushStyleColor(ImGuiCol.TextSelectedBg, Colors.reAlpha(theme.iconHighlight, Controls.FIELD_SELECTION_ALPHA));
        ImGui.pushStyleColor(ImGuiCol.NavCursor, 0);
        ImGui.beginDisabled(!open);
        if (focusSearch) ImGui.setKeyboardFocusHere();
        if (ImGui.inputTextWithHint("##search", SEARCH_HINT, searchBuffer) && open) panel.setSearch(searchBuffer.get());
        float focus = open && (ImGui.isItemActive() || ImGui.isItemHovered()) ? 1 : 0;
        if (ImGui.isItemActive()) focusSearch = false;
        searchFocus += (focus - searchFocus) * blend;
        ImGui.endDisabled();
        ImGui.popStyleColor(7);
        ImGui.popStyleVar(4);
        ImGui.popClipRect();
        draw.addRect(minX, minY, right, maxY, theme.split, rounding, 0, scale(1));
        Widgets.popFont();
    }

    private static float[] ink(String text) {
        float top = Widgets.textHeight();
        float bottom = 0;
        ImDrawList measure = ImGui.getForegroundDrawList();
        long list = measure.ptr;
        int first = Natives.vertexCount(list);
        measure.pushClipRect(-16384, -4096, -8192, 4096, false);
        ImFont font = ImGui.getFont();
        font.renderText(measure, ImGui.getFontSize(), -12288, 0, 0xFFFFFFFF, -16384, -4096, -8192, 4096, text, null, 0, 0);
        measure.popClipRect();
        if (Natives.vertexCount(list) > first) {
            float[] range = Natives.inkRange(list, first);
            top = Math.min(top, range[0]);
            bottom = Math.max(bottom, range[1]);
        }
        return new float[]{top, bottom};
    }

    private static float wordmarkWidth(String text) {
        float width = 0;
        float tracking = ImGui.getFontSize() * (WORDMARK_TRACKING / WORDMARK_SIZE);
        for (int i = 0; i < text.length(); ) {
            int end = text.offsetByCodePoints(i, 1);
            width += ImGui.calcTextSizeX(text.substring(i, end)) + tracking;
            i = end;
        }
        return width > 0 ? width - tracking : 0;
    }

    private static void drawWordmark(ImDrawList draw, String text, float x, float y, int color) {
        for (int i = 0; i < text.length(); ) {
            int end = text.offsetByCodePoints(i, 1);
            String glyph = text.substring(i, end);
            draw.addText(x, y, color, glyph);
            x += ImGui.calcTextSizeX(glyph) + ImGui.getFontSize() * (WORDMARK_TRACKING / WORDMARK_SIZE);
            i = end;
        }
    }

    private static void drawTitle(ImDrawList draw, String text, float x, float y, float animation, float offset) {
        if (animation > 0) {
            ImVec2[] first = {new ImVec2(0, 0), new ImVec2(-offset, -offset), new ImVec2(offset, -offset), new ImVec2(-offset, offset), new ImVec2(offset, offset)};
            ImVec2[] second = {new ImVec2(0, 0), new ImVec2(offset, offset), new ImVec2(-offset, offset), new ImVec2(offset, -offset), new ImVec2(-offset, -offset)};
            float segmentFloat = animation * 4.0F;
            int segment = Math.min((int) segmentFloat, 3);
            float t = segmentFloat - segment;
            t = t * t * (3.0F - 2.0F * t);
            drawWordmark(draw, text, x + first[segment].x + (first[segment + 1].x - first[segment].x) * t,
                    y + first[segment].y + (first[segment + 1].y - first[segment].y) * t, LOGO_FIRST);
            drawWordmark(draw, text, x + second[segment].x + (second[segment + 1].x - second[segment].x) * t,
                    y + second[segment].y + (second[segment + 1].y - second[segment].y) * t, LOGO_SECOND);
        }
        drawWordmark(draw, text, x, y, LOGO_MAIN);
    }

    private static boolean actionItem(String label, Icon icon, int textColor, int iconColor, int hover) {
        float width = scale(POPUP_WIDTH) - scale(POPUP_INSET) * 2.0F;
        float minX = ImGui.getCursorScreenPosX();
        float minY = ImGui.getCursorScreenPosY();
        ImGui.invisibleButton(label, width, scale(POPUP_ACTION));
        boolean hovered = ImGui.isItemHovered();
        boolean clicked = ImGui.isItemClicked();
        ImDrawList draw = ImGui.getWindowDrawList();
        if (hovered) draw.addRectFilled(minX, minY, minX + width, minY + scale(POPUP_ACTION), hover, scale(POPUP_ROUNDING));
        icon.draw(draw, minX + scale(13), minY + scale(POPUP_ACTION) / 2.0F, scale(13), iconColor);
        Widgets.pushText(Widgets.TEXT_BODY);
        draw.addText(minX + scale(28), minY + (scale(POPUP_ACTION) - Widgets.textHeight()) / 2.0F, textColor, label);
        Widgets.popFont();
        return clicked;
    }

    private static void file(ImDrawList draw, float x, float y, float size, int color) {
        float half = size / 2.0F;
        float fold = size * 0.32f;
        ImVec2[] points = {
                new ImVec2(x - half, y - half),
                new ImVec2(x + half - fold, y - half),
                new ImVec2(x + half, y - half + fold),
                new ImVec2(x + half, y + half),
                new ImVec2(x - half, y + half)
        };
        draw.addConvexPolyFilled(points, points.length, color);
    }

    private static void folder(ImDrawList draw, float x, float y, float size, int color) {
        float half = size / 2.0F;
        float minX = x - half;
        float minY = y - half * 0.65f;
        draw.addRectFilled(minX + scale(1), minY - size * 0.28f, minX + size * 0.5f, minY + scale(1), color, scale(1));
        draw.addRectFilled(minX, minY, x + half, y + half * 0.85f, color, scale(1.5f));
    }

    private static void pencil(ImDrawList draw, float x, float y, float size, int color) {
        float half = size / 2.0F;
        float tipX = x - half;
        float tipY = y + half;
        draw.addLine(tipX, tipY, x + half, y - half, color, size * 0.28f);
        draw.addTriangleFilled(tipX, tipY, tipX, tipY - size * 0.36f, tipX + size * 0.36f, tipY, color);
    }

    private static void copy(ImDrawList draw, float x, float y, float size, int color) {
        float half = size / 2.0F;
        draw.addRect(x - half, y - half, x + half * 0.35f, y + half * 0.35f, color, scale(1.5f), 0, scale(1.6f));
        draw.addRectFilled(x - half * 0.35f, y - half * 0.35f, x + half, y + half, color, scale(1.5f));
    }

    private static void trash(ImDrawList draw, float x, float y, float size, int color) {
        float half = size / 2.0F;
        float lid = y - half * 0.45f;
        draw.addRectFilled(x - half, lid, x + half, lid + size * 0.15f, color, scale(1));
        draw.addRectFilled(x - half * 0.4f, y - half, x + half * 0.4f, lid, color, scale(1));
        draw.addRectFilled(x - half * 0.78f, lid + size * 0.24f, x + half * 0.78f, y + half, color, size * 0.14f);
    }

    private static void plus(ImDrawList draw, float x, float y, float size, int color) {
        float half = size / 2.0F;
        draw.addLine(x - half, y, x + half, y, color, scale(1.8f));
        draw.addLine(x, y - half, x, y + half, color, scale(1.8f));
    }

    private static void dots(ImDrawList draw, float x, float y, float size, int color) {
        for (int index = -1; index <= 1; index++) draw.addCircleFilled(x + index * size * 0.34f, y, size * 0.10f, color);
    }

    private static void magnifier(ImDrawList draw, float x, float y, float size, int color) {
        float unit = size / 14.0F;
        draw.addCircle(x, y, 5.0F * unit, color, 0, 2.0F * unit);
        draw.addLine(x + 4 * unit, y + 4 * unit, x + 8 * unit, y + 8 * unit, color, 2.0F * unit);
    }

    @FunctionalInterface
    private interface Icon {
        void draw(ImDrawList draw, float x, float y, float size, int color);
    }

    private static final class Action {
        private int renameIndex = -1;
        private String target = "";
        private boolean overwrite;
        private boolean duplicate;
        private boolean remove;
        private boolean refresh;
        private boolean openFolder;
    }
}
