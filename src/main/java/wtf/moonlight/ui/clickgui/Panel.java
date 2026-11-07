package wtf.moonlight.ui.clickgui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import wtf.moonlight.bind.BindCapture;
import wtf.moonlight.config.ConfigManager;
import wtf.moonlight.module.ModuleManager;
import wtf.moonlight.ui.imgui.Effects;
import wtf.moonlight.ui.imgui.Fonts;
import wtf.moonlight.ui.imgui.ImGuiManager;
import wtf.moonlight.ui.imgui.Natives;

import java.util.function.Supplier;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class Panel {
    private static final float WIDTH = 748;
    private static final float HEIGHT = 576;
    private static final float ROUNDING = 14;

    private final ModuleManager modules;
    private final ConfigManager configs;
    private final ClientSettings settings;
    private final BindCapture capture;
    private final Supplier<String> user;
    private final Popups popups = new Popups();
    private final Placement placement = new Placement();
    private final TopBar topBar = new TopBar(this);
    private final PageLayout layout = new PageLayout(this);
    private float renderedScale;
    private float appliedScale;
    private int lastFrame = -2;
    private double revealStart;
    private float pageMix = 1;
    private boolean searching;
    private String search = "";
    private boolean closePopups;

    public Panel(ModuleManager modules, ConfigManager configs, ClientSettings settings, BindCapture capture, Supplier<String> user) {
        this.modules = modules;
        this.configs = configs;
        this.settings = settings;
        this.capture = capture;
        this.user = user;
    }

    public Theme colors() {
        return settings.colors();
    }

    public void frame(boolean open) {
        if (closePopups) {
            closePopups = false;
            ImGui.openPopup("##closed");
            imgui.internal.ImGui.clearActiveID();
        }
        if (open) render();
    }

    void close() {
        capture.cancel();
        closePopups = true;
    }

    ModuleManager modules() {
        return modules;
    }

    ConfigManager configs() {
        return configs;
    }

    ClientSettings settings() {
        return settings;
    }

    BindCapture capture() {
        return capture;
    }

    String user() {
        return user.get();
    }

    boolean isSearching() {
        return searching;
    }

    void setSearching(boolean value) {
        searching = value;
        if (!value) search = "";
    }

    String getSearch() {
        return search;
    }

    void setSearch(String value) {
        search = value;
    }

    void pageChanged() {
        pageMix = 0;
    }

    float pageReveal() {
        pageMix = pageMix + (1.0F - pageMix) * (1.0F - (float) Math.exp(-15.0F * ImGui.getIO().getDeltaTime()));
        return 1.0F - (float) Math.pow(1.0F - pageMix, 3.0F);
    }

    private void render() {
        Theme theme = colors();
        Controls.setTheme(theme);
        if (lastFrame != ImGui.getFrameCount() - 1) {
            revealStart = ImGui.getTime();
            popups.reset();
        }
        lastFrame = ImGui.getFrameCount();
        float reveal = Math.clamp((float) ((ImGui.getTime() - revealStart) / .48), 0.0F, 1.0F);

        if (appliedScale == 0 || !ImGui.isMouseDown(ImGuiMouseButton.Left)) appliedScale = settings.guiScale();
        ImGuiIO io = ImGui.getIO();
        float fit = Math.min(Math.max(1.0F, io.getDisplaySizeX() - 16.0F) / WIDTH, Math.max(1.0F, io.getDisplaySizeY() - 16.0F) / HEIGHT);
        float scale = Math.min(appliedScale * ImGuiManager.displayScale(), fit);
        if (renderedScale != scale) {
            popups.reset();
            renderedScale = scale;
        }
        Popups.setActive(popups);
        Widgets.setScale(scale);
        Windows.reset();
        ImGuiManager.saveStyle();
        ImGuiStyle style = ImGui.getStyle();
        style.scaleAllSizes(scale);
        style.setFontScaleMain(style.getFontScaleMain() * scale);
        ImGui.pushFont(Fonts.regular(), 15);

        ImGui.setNextWindowSize(scale(WIDTH), scale(HEIGHT));
        placement.placeCentered(scale(WIDTH), scale(HEIGHT));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, scale(ROUNDING));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, scale(0), scale(0));
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, scale(0), scale(0));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, scale(1));
        ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarSize, scale(12));
        ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarPadding, scale(3));
        ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarRounding, scale(6));
        ImGui.pushStyleColor(ImGuiCol.WindowBg, theme.windowBg);
        ImGui.pushStyleColor(ImGuiCol.Border, theme.windowBorder);
        ImGui.pushStyleColor(ImGuiCol.ScrollbarBg, theme.scrollbarTrack);
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrab, theme.scrollbarThumb);
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrabHovered, theme.scrollbarThumbHover);
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrabActive, theme.scrollbarThumbActive);

        boolean visible = Windows.begin("PanelClickGui", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.NoBackground);
        placement.capture();
        if (visible) {
            float x = ImGui.getWindowPosX();
            float y = ImGui.getWindowPosY();
            float maxX = x + ImGui.getWindowSizeX();
            float maxY = y + ImGui.getWindowSizeY();
            ImGui.pushClipRect(x, y, maxX, maxY, false);
            ImGui.pushStyleVar(ImGuiStyleVar.DisabledAlpha, 1);
            ImGui.beginDisabled(reveal < 1);
            ImDrawList draw = ImGui.getWindowDrawList();
            draw.pushClipRectFullScreen();
            Effects.shadow(draw, x, y, maxX, maxY, scale(ROUNDING), 0, Controls.SHADOW);
            Effects.blur(draw, x, y, maxX, maxY, scale(ROUNDING), 0);
            draw.popClipRect();
            draw.pushClipRect(x - 1, y - 1, maxX + 1, maxY + 1, false);
            Controls.glass(draw, x, y, maxX, maxY, theme.windowBg, scale(ROUNDING));
            draw.addRect(x, y, maxX, maxY, theme.windowBorder, scale(ROUNDING), 0, scale(1));
            draw.popClipRect();
            draw.addRectFilled(x, y, x + scale(158), y + scale(576), theme.categoryPanelBg, scale(14), ImDrawFlags.RoundCornersLeft);
            draw.addLine(x + scale(158), y, x + scale(158), y + scale(576), theme.windowBorder, scale(1));
            topBar.render();
            layout.navigation(x, y);
            layout.content(x, y);
            layout.account(x, y);
            ImGui.endDisabled();
            ImGui.popStyleVar();
            ImGui.popClipRect();
            if (reveal < 1) {
                float e = 1 - (float) Math.pow(1 - reveal, 4.0F);
                float zoom = .92f + (1 - .92f) * e;
                float pivotX = x + scale(750) * .5f;
                float pivotY = y + scale(756) * .5f;
                for (long list : Windows.all()) Natives.transform(list, pivotX, pivotY, zoom, 0, scale(16) * (1 - e), e);
            }
        }
        Windows.end();

        ImGui.popStyleColor(6);
        ImGui.popStyleVar(7);
        ImGuiManager.restoreStyle();
        ImGui.popFont();
        popups.drawClosing();
        Popups.setActive(null);
        Controls.setTheme(null);
        Widgets.setScale(1);
    }
}
