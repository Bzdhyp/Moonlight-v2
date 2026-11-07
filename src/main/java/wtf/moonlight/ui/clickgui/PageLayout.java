package wtf.moonlight.ui.clickgui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiStorage;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.Subcategory;
import wtf.moonlight.ui.clickgui.element.Palette;
import wtf.moonlight.ui.clickgui.element.SliderElement;
import wtf.moonlight.ui.imgui.Textures;
import wtf.moonlight.value.Value;
import wtf.moonlight.value.impl.ColorValue;
import wtf.moonlight.value.impl.ModeValue;
import wtf.moonlight.value.impl.NumberValue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

final class PageLayout {
    static final int CLIENT_PAGE = -1;

    private static final int SUB_PAGE = 100;
    private static final float NAV_ROW = 36;
    private static final float NAV_MAX = 290;
    private static final float SUB_INDENT = 16;
    private static final float MENU_WIDTH = 280;
    private static final float MENU_ROW = 32;
    private static final float MENU_TOP = 64;
    private static final int EXPAND = 3;
    private static final float CARD_TITLE = 20;
    private static final float CARD_GAP = 12;
    private static final String BETA = "Beta";
    private static final String SETTINGS_ICON = Character.toString(0xF013);

    private final Panel panel;
    private final Set<Category> expanded = EnumSet.noneOf(Category.class);
    private final List<Group> groups = new ArrayList<>();
    private final Map<NumberValue, SliderElement> sliders = new HashMap<>();
    private List<Module> shown = List.of();
    private boolean builtClient;
    private int page;

    PageLayout(Panel panel) {
        this.panel = panel;
    }

    int page() {
        return page;
    }

    void changePage(int page) {
        panel.setSearching(false);
        if (this.page == page) return;
        this.page = page;
        panel.pageChanged();
        panel.setSearch("");
        panel.setSearching(false);
    }

    void navigation(float originX, float originY) {
        Theme theme = panel.colors();
        ImDrawList draw = ImGui.getWindowDrawList();
        Widgets.label(draw, originX + scale(16), originY + scale(67), theme.secondary, "MODULES", 10);
        Category[] categories = Category.values();
        float[] open = new float[categories.length];
        float content = 0;
        for (Category category : categories) {
            open[category.ordinal()] = Widgets.motion(0x3e70 + category.ordinal(), expanded.contains(category) ? 1 : 0, 14);
            content += NAV_ROW * (1 + subcategories(category).size() * open[category.ordinal()]);
        }
        float navHeight = Math.min(NAV_MAX, content);
        if (content <= NAV_MAX) {
            categories(theme, originX, originY, 84, open);
        } else {
            ImGui.setCursorScreenPos(originX, originY + scale(84));
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
            Windows.beginChild("category-navigation", scale(154), scale(navHeight), ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar);
            categories(theme, originX, originY, 84 - ImGui.getScrollY() / scale(1), open);
            Windows.endChild();
            ImGui.popStyleVar();
        }
        Widgets.label(draw, originX + scale(16), originY + scale(97 + navHeight), theme.secondary, "CLIENT", 10);
        if (nav(theme, originX + scale(7), originY + scale(111 + navHeight), scale(140), -1, SETTINGS_ICON, "Settings", page == CLIENT_PAGE, true)) {
            changePage(CLIENT_PAGE);
        }
    }

    private void categories(Theme theme, float originX, float originY, float top, float[] open) {
        float y = top;
        for (Category category : Category.values()) {
            List<Subcategory> subs = subcategories(category);
            int id = category.ordinal();
            if (subs.isEmpty()) {
                if (nav(theme, originX + scale(7), originY + scale(y), scale(140), id, icon(category), category.getName(), page == id, true)) changePage(id);
                y += NAV_ROW;
                continue;
            }
            Subcategory current = page >= SUB_PAGE ? Subcategory.values()[page - SUB_PAGE] : null;
            boolean inside = current != null && current.getCategory() == category;
            if (nav(theme, originX + scale(7), originY + scale(y), scale(140), id, icon(category), category.getName(), inside, false)) {
                if (!expanded.remove(category)) {
                    expanded.add(category);
                    if (!inside) changePage(SUB_PAGE + subs.getFirst().ordinal());
                }
            }
            y += NAV_ROW;
            float height = NAV_ROW * subs.size() * open[id];
            if (height < 0.5F) continue;
            ImGui.pushClipRect(originX, originY + scale(y), originX + scale(158), originY + scale(y + height), true);
            for (int i = 0; i < subs.size(); i++) {
                Subcategory sub = subs.get(i);
                int target = SUB_PAGE + sub.ordinal();
                if (nav(theme, originX + scale(7 + SUB_INDENT), originY + scale(y + NAV_ROW * i), scale(140 - SUB_INDENT), target,
                        icon(sub), sub.getName(), page == target, true)) {
                    changePage(target);
                }
            }
            ImGui.popClipRect();
            y += height;
        }
    }

    void content(float originX, float originY) {
        rebuild();
        Theme theme = panel.colors();
        ImGui.setCursorScreenPos(originX + scale(158), originY + scale(56));
        Windows.beginChild("content", scale(590), scale(520), ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        if (groups.isEmpty()) {
            Widgets.label(ImGui.getWindowDrawList(), originX + scale(180), originY + scale(88), theme.secondary, "No matching modules", 14);
        }
        int columns = builtClient ? 1 : 2;
        for (int column = 0; column < columns; column++) {
            ImGui.pushID(column);
            ImGui.setCursorScreenPos(originX + scale(167 + 290 * column), originY + scale(64));
            Windows.beginChild("module-column", scale(builtClient ? 568 : 278), scale(497), ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar);
            int key = ImGui.getID("##list-generation");
            int generation = Objects.hash(panel.getSearch(), page);
            ImGuiStorage storage = ImGui.getStateStorage();
            if (storage.getInt(key) != generation) {
                ImGui.setScrollY(0);
                storage.setInt(key, generation);
            }
            for (Group group : groups) {
                if (group.column == column) card(theme, group, ImGui.getWindowWidth());
            }
            Windows.endChild();
            ImGui.popID();
        }
        ImGui.popStyleVar();
        Controls.pageReveal(panel.pageReveal());
        Windows.endChild();
    }

    void account(float originX, float originY) {
        Theme theme = panel.colors();
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = originX + scale(7);
        float y = originY + scale(531);
        ImGui.setCursorScreenPos(x, y);
        if (ImGui.invisibleButton("account", scale(140), scale(38))) ImGui.openPopup("account-popup");
        float response = Widgets.motion(ImGui.getItemID() ^ 0x1932, ImGui.isPopupOpen("account-popup") ? 1 : ImGui.isItemHovered() ? .5f : 0, 16);
        draw.addRectFilled(x, y, x + scale(140), y + scale(38), Colors.reAlpha(theme.categoryHighlight, (int) (235 * response)), scale(6));
        avatar(draw, theme, x + scale(21), y + scale(19), scale(14));
        String user = panel.user();
        draw.pushClipRect(x, y, x + scale(136), y + scale(38), true);
        Widgets.label(draw, x + scale(43), y + scale(4), theme.clientName, user, 14);
        Widgets.label(draw, x + scale(43), y + scale(20), theme.secondary, BETA, 12);
        draw.popClipRect();
        float height = MENU_TOP + MENU_ROW * 7 + 6;
        ImGui.setNextWindowPos(originX + scale(151), originY + scale(569 - height), ImGuiCond.Always);
        ImGui.setNextWindowSize(scale(MENU_WIDTH), scale(height));
        if (Widgets.beginPopup(theme, "account-popup", 0, Popups.Motion.ACCOUNT, 4, 4)) {
            accountMenu(theme, user);
            Widgets.endPopup();
        }
    }

    private void accountMenu(Theme theme, String user) {
        ClientSettings settings = panel.settings();
        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        ImDrawList draw = ImGui.getWindowDrawList();
        avatar(draw, theme, x + scale(28), y + scale(30), scale(17));
        Widgets.label(draw, x + scale(54), y + scale(12), theme.clientName, user, 15);
        Widgets.label(draw, x + scale(54), y + scale(31), theme.secondary, BETA, 12);
        draw.addLine(x + scale(14), y + scale(MENU_TOP - 6), x + scale(MENU_WIDTH - 14), y + scale(MENU_TOP - 6), theme.split, scale(1));
        sliderRow(theme, 0, 0xF00A, "Menu Scale", settings.menuSize());
        sliderRow(theme, 1, 0xF108, "HUD Scale", settings.hudSize());
        sliderRow(theme, 2, 0xF043, "HUD Alpha", settings.hudAlpha());
        sliderRow(theme, 3, 0xF042, "Background Alpha", settings.background());
        ModeValue mode = settings.theme();
        menuRow(theme, 4, 0xF53F, "Theme", "theme-mode");
        trailing(theme, 4, mode.getModes().get(mode.get()));
        themes(theme, mode);
        ColorValue color = settings.themeColor();
        menuRow(theme, 5, 0xF1FC, "Theme Color", "theme-color");
        float swatchX = x + scale(MENU_WIDTH - 30);
        float swatchY = y + scale(MENU_TOP + MENU_ROW * 5 + MENU_ROW * .5f);
        draw.addRectFilled(swatchX, swatchY - scale(7), swatchX + scale(14), swatchY + scale(7), color.get(), scale(3));
        submenu(5, Palette.HEIGHT);
        ImGui.setNextWindowSize(scale(Palette.WIDTH), scale(Palette.HEIGHT));
        if (Widgets.beginPopup(theme, "theme-color")) {
            Palette.draw(theme, color);
            Widgets.endPopup();
        }
        if (menuRow(theme, 6, 0xF013, "Client Settings", null)) {
            changePage(CLIENT_PAGE);
            ImGui.closeCurrentPopup();
        }
        trailing(theme, 6, "");
    }

    private boolean menuRow(Theme theme, int index, int icon, String label, @Nullable String popup) {
        float x = ImGui.getWindowPosX() + scale(4);
        float y = ImGui.getWindowPosY() + scale(MENU_TOP + MENU_ROW * index);
        ImGui.setCursorScreenPos(x, y);
        ImGui.pushID(index);
        boolean clicked = ImGui.invisibleButton("row", scale(MENU_WIDTH - 8), scale(MENU_ROW));
        boolean open = popup != null && ImGui.isPopupOpen(popup);
        float hover = Widgets.motion(ImGui.getItemID() ^ 0x5151, open ? 1 : ImGui.isItemHovered() ? .7f : 0, 22);
        ImGui.popID();
        if (clicked && popup != null) ImGui.openPopup(popup);
        rowBase(theme, index, icon, label, hover);
        return clicked;
    }

    private void sliderRow(Theme theme, int index, int icon, String label, NumberValue value) {
        float x = ImGui.getWindowPosX() + scale(4);
        float y = ImGui.getWindowPosY() + scale(MENU_TOP + MENU_ROW * index);
        float width = scale(MENU_WIDTH - 8);
        float middle = y + scale(MENU_ROW) * .5f;
        boolean hovered = ImGui.isWindowHovered() && ImGui.isMouseHoveringRect(x, y, x + width, y + scale(MENU_ROW));
        rowBase(theme, index, icon, label, Widgets.motion(ImGui.getID("##slider-row" + index), hovered ? .7f : 0, 22));
        SliderElement slider = sliders.computeIfAbsent(value, key -> new SliderElement(panel, key, true));
        ImGui.setCursorScreenPos(x + width - scale(12) - slider.width(), middle - slider.height() * .5f);
        ImGui.pushID(index);
        slider.draw();
        ImGui.popID();
    }

    private void rowBase(Theme theme, int index, int icon, String label, float hover) {
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = ImGui.getWindowPosX() + scale(4);
        float y = ImGui.getWindowPosY() + scale(MENU_TOP + MENU_ROW * index);
        float middle = y + scale(MENU_ROW) * .5f;
        draw.addRectFilled(x, y, x + scale(MENU_WIDTH - 8), y + scale(MENU_ROW),
                Colors.reAlpha(theme.selectorElementHover, (int) (Colors.alpha(theme.selectorElementHover) * hover)), scale(12));
        Widgets.pushIcon();
        draw.addText(x + scale(12), middle - Widgets.textHeight() * .5f, theme.main, Character.toString(icon));
        Widgets.popFont();
        Widgets.pushText(Widgets.TEXT_CONTROL);
        draw.addText(x + scale(36), middle - Widgets.textHeight() * .5f, theme.main, label);
        Widgets.popFont();
    }

    private void trailing(Theme theme, int index, String value) {
        ImDrawList draw = ImGui.getWindowDrawList();
        float right = ImGui.getWindowPosX() + scale(MENU_WIDTH - 4);
        float middle = ImGui.getWindowPosY() + scale(MENU_TOP + MENU_ROW * index + MENU_ROW * .5f);
        Widgets.pushText(Widgets.TEXT_CONTROL);
        draw.addText(right - scale(22) - Widgets.textWidth(value), middle - Widgets.textHeight() * .5f, theme.secondary, value);
        Widgets.popFont();
        Controls.chevron(draw, right - scale(14), middle - scale(3), theme.secondary);
    }

    private void themes(Theme theme, ModeValue mode) {
        List<String> modes = mode.getModes();
        submenu(4, 8 + MENU_ROW * modes.size());
        ImGui.setNextWindowSize(scale(120), scale(8 + MENU_ROW * modes.size()));
        if (!Widgets.beginPopup(theme, "theme-mode")) return;
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = ImGui.getWindowPosX() + scale(4);
        for (int i = 0; i < modes.size(); i++) {
            float y = ImGui.getWindowPosY() + scale(4 + MENU_ROW * i);
            ImGui.setCursorScreenPos(x, y);
            ImGui.pushID(i);
            boolean clicked = ImGui.invisibleButton("mode", scale(112), scale(MENU_ROW));
            float hover = Widgets.motion(ImGui.getItemID() ^ 0x4d4d, ImGui.isItemHovered() ? 1 : 0, 22);
            ImGui.popID();
            draw.addRectFilled(x, y, x + scale(112), y + scale(MENU_ROW),
                    Colors.reAlpha(theme.selectorElementHover, (int) (Colors.alpha(theme.selectorElementHover) * hover)), scale(12));
            Widgets.pushText(Widgets.TEXT_CONTROL);
            draw.addText(x + scale(14), y + (scale(MENU_ROW) - Widgets.textHeight()) * .5f,
                    i == mode.get() ? theme.textHighlight : Colors.mix(theme.secondary, theme.main, hover), modes.get(i));
            Widgets.popFont();
            if (clicked) {
                mode.set(i);
                ImGui.closeCurrentPopup();
            }
        }
        Widgets.endPopup();
    }

    private void submenu(int index, float height) {
        ImGuiViewport viewport = ImGui.getMainViewport();
        float y = Math.min(ImGui.getWindowPosY() + scale(MENU_TOP + MENU_ROW * index - 4),
                viewport.getWorkPosY() + viewport.getWorkSizeY() - scale(height + 12));
        ImGui.setNextWindowPos(ImGui.getWindowPosX() + scale(MENU_WIDTH + 6), y, ImGuiCond.Always);
    }

    private void avatar(ImDrawList draw, Theme theme, float x, float y, float radius) {
        draw.addCircleFilled(x, y, radius, theme.clientIconBg);
        Textures.face(draw, panel.skin(), x, y, radius, Colors.rgb(255, 255, 255));
    }

    private boolean nav(Theme theme, float x, float y, float width, int id, String icon, String name, boolean active, boolean fill) {
        ImDrawList draw = ImGui.getWindowDrawList();
        ImGui.pushID(id);
        ImGui.setCursorScreenPos(x, y);
        boolean clicked = ImGui.invisibleButton("nav", width, scale(30));
        boolean hovered = ImGui.isItemHovered();
        float response = Widgets.motion(ImGui.getItemID() ^ 0x7771, active ? 1 : hovered ? .48f : 0, 16);
        float surface = fill ? response : Widgets.motion(ImGui.getItemID() ^ 0x7773, hovered ? .48f : 0, 16);
        ImGui.popID();
        draw.addRectFilled(x, y, x + width, y + scale(30),
                Colors.interpolate(Colors.reAlpha(theme.categoryPanelBg, 0), theme.categoryHighlight, surface), scale(6));
        int iconColor = Colors.interpolate(theme.navIcon, fill ? theme.iconHighlight : theme.textHighlight, response);
        Widgets.pushIcon();
        draw.addText(x + scale(10), y + (scale(30) - Widgets.textHeight()) * .5f, iconColor, icon);
        Widgets.popFont();
        Widgets.pushText(Widgets.TEXT_BODY);
        draw.addText(x + scale(31), y + (scale(30) - Widgets.textHeight()) * .5f,
                Colors.interpolate(theme.navText, theme.textHighlight, response), name);
        Widgets.popFont();
        return clicked;
    }


    private static List<Subcategory> subcategories(Category category) {
        List<Subcategory> out = new ArrayList<>();
        for (Subcategory sub : Subcategory.values()) {
            if (sub.getCategory() == category) out.add(sub);
        }
        return out;
    }

    private void rebuild() {
        boolean client = page == CLIENT_PAGE && panel.getSearch().isEmpty();
        List<Module> modules = client ? List.of() : choices();
        if (modules.equals(shown) && client == builtClient && !groups.isEmpty()) return;
        shown = modules;
        builtClient = client;
        groups.clear();
        if (client) {
            Group group = new Group("Client", 0);
            for (Value<?> value : panel.settings().getValues()) group.rows.add(Row.of(panel, value, false));
            groups.add(group);
            return;
        }
        List<Row> toggles = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<List<Row>> cards = new ArrayList<>();
        for (Module module : shown) {
            if (module.getValues().stream().filter(Value::isVisible).count() < EXPAND) {
                toggles.add(Row.of(panel, module));
                continue;
            }
            List<Row> rows = new ArrayList<>();
            rows.add(Row.enabled(panel, module));
            for (Value<?> value : module.getValues()) rows.add(Row.of(panel, value, false));
            names.add(module.getName());
            cards.add(rows);
        }
        if (!toggles.isEmpty()) {
            names.addFirst("Toggles");
            cards.addFirst(toggles);
        }
        float[] heights = new float[2];
        for (int i = 0; i < cards.size(); i++) {
            int column = heights[0] <= heights[1] ? 0 : 1;
            Group group = new Group(names.get(i), column);
            group.rows.addAll(cards.get(i));
            groups.add(group);
            heights[column] += CARD_TITLE + CARD_GAP + 37 * group.rows.stream().filter(Row::isVisible).count();
        }
    }

    private List<Module> choices() {
        String search = panel.getSearch().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module module : panel.modules().getModules()) {
            if (!search.isEmpty()) {
                if (module.getName().toLowerCase(Locale.ROOT).contains(search)) out.add(module);
            } else if (page >= SUB_PAGE ? module.getSubcategory() == Subcategory.values()[page - SUB_PAGE] : module.getCategory().ordinal() == page) {
                out.add(module);
            }
        }
        return out;
    }

    private void card(Theme theme, Group group, float width) {
        ImDrawList draw = ImGui.getWindowDrawList();
        long count = group.rows.stream().filter(Row::isVisible).count();
        if (count == 0) return;
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY() + scale(CARD_TITLE);
        float height = scale(count * 37.0F);
        Controls.card(draw, x, y, width, height, group.name.toUpperCase(Locale.ROOT));
        ImGui.pushID(group.name);
        ImGui.setCursorScreenPos(x + scale(13), y);
        Windows.beginChild("card", width - scale(19), height, ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        int drawn = 0;
        for (Row row : group.rows) {
            if (!row.isVisible()) continue;
            if (drawn > 0) {
                float lineX = ImGui.getCursorScreenPosX();
                float lineY = ImGui.getCursorScreenPosY();
                ImGui.getWindowDrawList().addLine(lineX, lineY, lineX + width - scale(26), lineY, theme.split, scale(1));
            }
            row.render();
            drawn++;
        }
        Windows.endChild();
        ImGui.popID();
        ImGui.setCursorScreenPos(x, y + height + scale(CARD_GAP));
        ImGui.dummy(0, 0);
    }

    private static String icon(Category category) {
        return switch (category) {
            case COMBAT -> Character.toString(0xF05B);
            case MOVEMENT -> Character.toString(0xF8CC);
            case PLAYER -> Character.toString(0xF007);
            case RENDER -> Character.toString(0xF03E);
            case MISC -> Character.toString(0xF1DE);
        };
    }

    private static String icon(Subcategory sub) {
        return switch (sub) {
            case OVERLAY -> Character.toString(0xF5FD);
            case WORLD -> Character.toString(0xF1B2);
        };
    }

    private static final class Group {
        private final String name;
        private final int column;
        private final List<Row> rows = new ArrayList<>();

        private Group(String name, int column) {
            this.name = name;
            this.column = column;
        }
    }
}
