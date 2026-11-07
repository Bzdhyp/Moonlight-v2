package wtf.moonlight.ui.clickgui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiStorage;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;
import wtf.moonlight.ui.clickgui.element.BindElement;
import wtf.moonlight.ui.clickgui.element.SwitchElement;
import wtf.moonlight.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

final class PageLayout {
    static final int CLIENT_PAGE = -1;

    private static final String SETTINGS_ICON = Character.toString(0xF013);

    private final Panel panel;
    private final List<Group> groups = new ArrayList<>();
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
        float navHeight = Math.min(180, 36.0F * categories.length);
        if (categories.length <= 5) {
            for (int i = 0; i < categories.length; i++) {
                nav(theme, originX, originY, i, icon(categories[i]), categories[i].getName(), 84 + 36.0F * i, i);
            }
        } else {
            ImGui.setCursorScreenPos(originX, originY + scale(84));
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
            Windows.beginChild("category-navigation", scale(154), scale(navHeight), ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar);
            float scroll = ImGui.getScrollY() / scale(1);
            for (int i = 0; i < categories.length; i++) {
                nav(theme, originX, originY, i, icon(categories[i]), categories[i].getName(), 84 + 36.0F * i - scroll, i);
            }
            Windows.endChild();
            ImGui.popStyleVar();
        }
        Widgets.label(draw, originX + scale(16), originY + scale(97 + navHeight), theme.secondary, "CLIENT", 10);
        nav(theme, originX, originY, -1, SETTINGS_ICON, "Settings", 111 + navHeight, CLIENT_PAGE);
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
        ClientSettings settings = panel.settings();
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = originX + scale(7);
        float y = originY + scale(531);
        ImGui.setCursorScreenPos(x, y);
        if (ImGui.invisibleButton("account", scale(140), scale(38))) ImGui.openPopup("account-popup");
        float response = Widgets.motion(ImGui.getItemID() ^ 0x1932, ImGui.isPopupOpen("account-popup") ? 1 : ImGui.isItemHovered() ? .5f : 0, 16);
        draw.addRectFilled(x, y, x + scale(140), y + scale(38), Colors.reAlpha(theme.categoryHighlight, (int) (235 * response)), scale(6));
        draw.addCircleFilled(x + scale(21), y + scale(19), scale(14), theme.clientIconBg);
        String brand = settings.clientName();
        String initial = brand.substring(0, brand.offsetByCodePoints(0, 1));
        Widgets.pushText(14);
        draw.addText(x + scale(21) - Widgets.textWidth(initial) / 2.0F, y + scale(19) - Widgets.textHeight() / 2.0F, theme.clientIcon, initial);
        Widgets.popFont();
        String user = panel.user();
        draw.pushClipRect(x, y, x + scale(127), y + scale(38), true);
        Widgets.label(draw, x + scale(43), y + scale(4), theme.clientName, user, 14);
        Widgets.label(draw, x + scale(43), y + scale(20), theme.secondary, brand + " Client", 12);
        draw.popClipRect();
        Controls.chevron(draw, x + scale(132), y + scale(15), theme.main);
        ImGui.setNextWindowPos(originX + scale(151), originY + scale(380), ImGuiCond.Appearing);
        ImGui.setNextWindowSize(scale(218), scale(181));
        if (Widgets.beginPopup(theme, "account-popup", 0, Popups.Motion.ACCOUNT, 4, 4)) {
            float popupX = ImGui.getWindowPosX();
            float popupY = ImGui.getWindowPosY();
            ImDrawList popup = ImGui.getWindowDrawList();
            popup.addCircleFilled(popupX + scale(35), popupY + scale(34), scale(18), theme.clientIconBg);
            Widgets.label(popup, popupX + scale(64), popupY + scale(15), theme.clientName, user, 15);
            Widgets.label(popup, popupX + scale(64), popupY + scale(34), theme.iconHighlight, brand + " settings", 14);
            ImGui.setCursorPos(scale(4), scale(66));
            if (Widgets.choice(theme, "settings", "Client settings", page == CLIENT_PAGE, scale(210))) {
                changePage(CLIENT_PAGE);
                ImGui.closeCurrentPopup();
            }
            if (Widgets.choice(theme, "theme", "Light theme", settings.isLight(), scale(210))) settings.toggleTheme();
            if (Widgets.choice(theme, "scale", "Menu scale", false, scale(210))) {
                changePage(CLIENT_PAGE);
                ImGui.closeCurrentPopup();
            }
            Widgets.endPopup();
        }
    }

    private void nav(Theme theme, float originX, float originY, int id, String icon, String name, float offsetY, int target) {
        ImDrawList draw = ImGui.getWindowDrawList();
        float x = originX + scale(7);
        float y = originY + scale(offsetY);
        ImGui.pushID(id);
        ImGui.setCursorScreenPos(x, y);
        if (ImGui.invisibleButton("nav", scale(140), scale(30))) changePage(target);
        float response = Widgets.motion(ImGui.getItemID() ^ 0x7771, page == target ? 1 : ImGui.isItemHovered() ? .48f : 0, 16);
        draw.addRectFilled(x, y, x + scale(140), y + scale(30),
                Colors.interpolate(Colors.reAlpha(theme.categoryPanelBg, 0), theme.categoryHighlight, response), scale(6));
        int iconColor = Colors.interpolate(Colors.rgb(137, 142, 153), Colors.rgb(82, 141, 255), response);
        Widgets.pushIcon();
        draw.addText(x + scale(10), y + (scale(30) - Widgets.textHeight()) * .5f, iconColor, icon);
        Widgets.popFont();
        Widgets.pushText(Widgets.TEXT_BODY);
        draw.addText(x + scale(31), y + (scale(30) - Widgets.textHeight()) * .5f,
                Colors.interpolate(Colors.rgb(145, 149, 159), theme.textHighlight, response), name);
        Widgets.popFont();
        ImGui.popID();
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
            for (Value<?> value : panel.settings().getValues()) group.rows.add(Row.of(panel, value));
            groups.add(group);
            return;
        }
        List<Row> rows = new ArrayList<>();
        for (Module module : shown) rows.add(moduleRow(module));
        int limit = Math.max(1, (rows.size() + 1) / 2);
        int placed = 0;
        for (int start = 0; start < rows.size(); ) {
            Group part = new Group("General", placed < limit ? 0 : 1);
            int count = Math.min(rows.size() - start, part.column == 0 ? limit - placed : rows.size() - start);
            part.rows.addAll(rows.subList(start, start + count));
            groups.add(part);
            start += count;
            placed += count;
        }
    }

    private Row moduleRow(Module module) {
        List<Row> children = new ArrayList<>();
        children.add(new Row(panel, "Bind", new BindElement(panel, module.getBind(), panel.capture()), List.of(), () -> true));
        for (Value<?> value : module.getValues()) children.add(Row.of(panel, value));
        return new Row(panel, module.getName(), new SwitchElement(panel, module::isEnabled, module::setEnabled), children, () -> true);
    }

    private List<Module> choices() {
        String search = panel.getSearch().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module module : panel.modules().getModules()) {
            if (!search.isEmpty()) {
                if (module.getName().toLowerCase(Locale.ROOT).contains(search)) out.add(module);
            } else if (module.getCategory().ordinal() == page) {
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
        float y = ImGui.getCursorScreenPosY() + scale(20);
        float height = scale(count * 37.0F);
        Controls.card(draw, x, y, width, height, group.name);
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
        ImGui.setCursorScreenPos(x, y + height + scale(12));
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
