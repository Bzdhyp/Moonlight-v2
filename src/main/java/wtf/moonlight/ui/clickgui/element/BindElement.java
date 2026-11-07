package wtf.moonlight.ui.clickgui.element;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import wtf.moonlight.bind.Bind;
import wtf.moonlight.bind.BindCapture;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Theme;
import wtf.moonlight.ui.clickgui.Widgets;
import wtf.moonlight.utils.KeyUtils;

import static wtf.moonlight.ui.clickgui.Widgets.scale;

public final class BindElement extends Element {
    private static final float ICON_SIZE = 14;
    private static final String GLYPH = Character.toString(0xF11C);

    private final Bind bind;
    private final BindCapture capture;

    public BindElement(Panel panel, Bind bind, BindCapture capture) {
        super(panel);
        this.bind = bind;
        this.capture = capture;
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
        if (ImGui.invisibleButton("bind", scale(ICON_SIZE), scale(ICON_SIZE))) ImGui.openPopup("bindpopup");
        Widgets.pushIcon();
        float glyphWidth = ImGui.calcTextSizeX(GLYPH);
        float glyphHeight = ImGui.calcTextSizeY(GLYPH);
        ImGui.getWindowDrawList().addText(ImGui.getItemRectMinX() + (scale(ICON_SIZE) - glyphWidth) / 2.0F,
                ImGui.getItemRectMinY() + (scale(ICON_SIZE) - glyphHeight) / 2.0F - scale(0.5f),
                bind.isActive() ? theme.iconHighlight : theme.main, GLYPH);
        Widgets.popFont();
        drawPopup(theme);
    }

    private void drawPopup(Theme theme) {
        ImGui.pushStyleVar(ImGuiStyleVar.PopupRounding, scale(12));
        ImGui.pushStyleVar(ImGuiStyleVar.PopupBorderSize, scale(1));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, scale(4), scale(4));
        ImGui.pushStyleColor(ImGuiCol.PopupBg, theme.modulePanelBg);
        ImGui.pushStyleColor(ImGuiCol.Border, theme.windowBorder);
        if (Widgets.beginPopup(theme, "bindpopup")) {
            ImGui.pushStyleColor(ImGuiCol.Button, theme.buttonBg);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, theme.buttonActiveBg);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, theme.buttonHoveredBg);
            ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
            ImGui.beginGroup();
            ImGui.dummy(0, scale(4));
            ImGui.beginGroup();
            ImGui.dummy(scale(4), 0);
            ImGui.sameLine();
            Widgets.pushText(Widgets.TEXT_BODY);
            ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, scale(5));
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, scale(6), scale(1));
            if (ImGui.button(bind.isHold() ? "Hold###bindtype" : "Click###bindtype")) bind.setHold(!bind.isHold());
            ImGui.sameLine(0, scale(4));
            boolean listening = capture.getTarget() == bind;
            ImGui.button((listening ? "LISTENING" : KeyUtils.getName(bind.getKey())) + "###bindkey");
            if (ImGui.isItemActivated()) {
                if (listening) {
                    bind.setKey(KeyUtils.NONE);
                    capture.cancel();
                } else {
                    capture.begin(bind, target -> {
                    }, () -> {
                    });
                }
            }
            ImGui.sameLine(0, scale(4));
            if (ImGui.button("Delete###bindclear")) {
                bind.setKey(KeyUtils.NONE);
                if (listening) capture.cancel();
            }
            ImGui.sameLine();
            ImGui.dummy(scale(4), 0);
            ImGui.popStyleVar(2);
            Widgets.popFont();
            ImGui.endGroup();
            ImGui.dummy(0, scale(4));
            ImGui.endGroup();
            ImGui.popStyleVar();
            ImGui.popStyleColor(4);
            Widgets.endPopup();
        }
        ImGui.popStyleColor(2);
        ImGui.popStyleVar(3);
    }
}
