package wtf.moonlight.ui.clickgui.element;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.type.ImString;
import wtf.moonlight.ui.clickgui.Colors;
import wtf.moonlight.ui.clickgui.Controls;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.clickgui.Theme;
import wtf.moonlight.ui.clickgui.Widgets;
import wtf.moonlight.value.impl.NumberValue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

import static wtf.moonlight.ui.clickgui.Widgets.scale;
import static wtf.moonlight.ui.clickgui.Widgets.textWidth;

public final class SliderElement extends Element {
    private final NumberValue value;
    private final ImString buffer = new ImString(63);
    private boolean editing;
    private boolean focus;
    private float editReveal;
    private int lastFrame = -2;

    public SliderElement(Panel panel, NumberValue value) {
        super(panel);
        this.value = value;
    }

    @Override
    public float width() {
        return scale(132);
    }

    @Override
    public float height() {
        return scale(21);
    }

    @Override
    public void draw() {
        Theme theme = panel.colors();
        if (lastFrame != ImGui.getFrameCount() - 1) {
            editing = focus = false;
            editReveal = 0;
        }
        lastFrame = ImGui.getFrameCount();
        float target = editing ? 1 : 0;
        editReveal += (target - editReveal) * (1 - (float) Math.exp(-22 * ImGui.getIO().getDeltaTime()));
        if (Math.abs(editReveal - target) < .001f) editReveal = target;
        Widgets.pushText(Widgets.TEXT_CONTROL);
        float labelWidth = Math.max(textWidth(value.format(value.get())), Math.max(textWidth(value.format(value.getMin())), textWidth(value.format(value.getMax()))));
        float restingWidth = Math.clamp(labelWidth + scale(8), scale(42), scale(90));
        float valueWidth = restingWidth + editReveal * (Math.max(restingWidth, scale(58)) - restingWidth);
        float trackWidth = 132.0F - valueWidth / scale(1) - 10.0F;
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        ImDrawList draw = ImGui.getWindowDrawList();
        double span = value.getMax() - value.getMin();
        float amount = span > 0 ? (float) ((value.get() - value.getMin()) / span) : 0;
        float changed = Controls.slider(draw, 0, x, y + scale(10), amount, amount, trackWidth);
        if (changed != amount) value.set(value.getMin() + changed * span);
        ImGui.setCursorScreenPos(x + scale(132) - valueWidth, y);
        float valueX = ImGui.getCursorScreenPosX();
        float valueY = ImGui.getCursorScreenPosY();
        float valueEndX = valueX + valueWidth;
        float valueEndY = valueY + scale(21);
        boolean hovered = ImGui.isWindowHovered() && ImGui.isMouseHoveringRect(valueX, valueY, valueEndX, valueEndY);
        float response = Widgets.motion(ImGui.getID("##value-hover"), hovered || editing ? 1 : 0, 16);
        int background = Colors.mix(theme.selectorBg, theme.buttonHoveredBg, response * Controls.FIELD_HOVER_FILL);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, scale(4), Math.max(0, (scale(21) - Widgets.textHeight()) * .5f));
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, scale(5));
        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 0);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, background);
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, background);
        ImGui.pushStyleColor(ImGuiCol.FrameBgActive, background);
        ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
        ImGui.pushStyleColor(ImGuiCol.TextSelectedBg, Colors.reAlpha(theme.iconHighlight, Controls.FIELD_SELECTION_ALPHA));
        ImGui.pushStyleColor(ImGuiCol.NavCursor, 0);
        if (editing) {
            ImGui.setNextItemWidth(valueWidth);
            if (focus) {
                ImGui.setKeyboardFocusHere();
                focus = false;
            }
            boolean entered = ImGui.inputText("##value-edit", buffer,
                    ImGuiInputTextFlags.EnterReturnsTrue | ImGuiInputTextFlags.AutoSelectAll | ImGuiInputTextFlags.CharsScientific);
            if (entered || ImGui.isItemDeactivatedAfterEdit()) {
                Double parsed = parse(buffer.get());
                if (parsed != null) value.set(parsed);
                editing = false;
            }
            if (ImGui.isItemDeactivated()) editing = false;
        } else {
            float px = ImGui.getCursorScreenPosX();
            float py = ImGui.getCursorScreenPosY();
            if (ImGui.invisibleButton("value", valueWidth, scale(21))) {
                editing = focus = true;
                buffer.set(formatG(value.get()));
            }
            draw.addRectFilled(px, py, px + valueWidth, py + scale(21), background, scale(5));
            String label = value.format(value.get());
            draw.pushClipRect(px, py, px + valueWidth, py + scale(21), true);
            float fit = Math.min(1, (valueWidth - scale(8)) / Math.max(1, textWidth(label)));
            float fontSize = ImGui.getFontSize() * fit;
            float centered = (valueWidth - textWidth(label) * fit) * .5f;
            Controls.addText(draw, ImGui.getFont(), fontSize, px + centered + editReveal * (scale(4) - centered),
                    py + (scale(21) - fontSize) * .5f, theme.main, label);
            draw.popClipRect();
            if (ImGui.isItemHovered()) {
                Widgets.pushText(Widgets.TEXT_SMALL);
                ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, scale(8), scale(5));
                ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, scale(5));
                ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, scale(1));
                ImGui.pushStyleColor(ImGuiCol.PopupBg, theme.selectorBg);
                ImGui.pushStyleColor(ImGuiCol.Border, theme.split);
                ImGui.pushStyleColor(ImGuiCol.Text, theme.main);
                ImGui.beginTooltip();
                ImGui.textUnformatted(label + " (click to edit)");
                ImGui.endTooltip();
                ImGui.popStyleColor(3);
                ImGui.popStyleVar(3);
                Widgets.popFont();
            }
        }
        draw.addRect(valueX, valueY, valueEndX, valueEndY,
                Colors.mix(theme.split, theme.iconHighlight, response * Controls.FIELD_HOVER_BORDER), scale(5), 0, scale(1));
        ImGui.popStyleColor(6);
        ImGui.popStyleVar(3);
        Widgets.popFont();
    }

    private static Double parse(String text) {
        if (text.isEmpty() || Character.isWhitespace(text.charAt(text.length() - 1))) return null;
        char last = Character.toLowerCase(text.charAt(text.length() - 1));
        if (last == 'd' || last == 'f') return null;
        try {
            double parsed = Double.parseDouble(text);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String formatG(double number) {
        if (number == 0 || !Double.isFinite(number)) return number == 0 ? "0" : String.valueOf(number);
        BigDecimal rounded = new BigDecimal(number).round(new MathContext(8));
        int exponent = rounded.precision() - rounded.scale() - 1;
        if (exponent < -4 || exponent >= 8) {
            String mantissa = rounded.movePointLeft(exponent).stripTrailingZeros().toPlainString();
            return String.format(Locale.ROOT, "%se%s%02d", mantissa, exponent < 0 ? "-" : "+", Math.abs(exponent));
        }
        return rounded.stripTrailingZeros().toPlainString();
    }
}
