package wtf.moonlight.ui.clickgui;

import static wtf.moonlight.ui.clickgui.Colors.rgb;
import static wtf.moonlight.ui.clickgui.Colors.rgba;

public final class Theme {
    public static final int ACCENT = rgb(75, 126, 255);

    public int clientIconBg;
    public int clientName;
    public int windowBg;
    public int windowBorder;
    public int categoryPanelBg;
    public int main;
    public int secondary;
    public int textHighlight;
    public int iconHighlight;
    public int categoryHighlight;
    public int modulePanelBg;
    public int split;
    public int switchDisabledBg;
    public int buttonBg;
    public int buttonHoveredBg;
    public int buttonActiveBg;
    public int sliderBg;
    public int selectorBg;
    public int selectorElementHover;
    public int selectorElementActive;
    public int scrollbarTrack;
    public int scrollbarThumb;
    public int scrollbarThumbHover;
    public int scrollbarThumbActive;
    public int navText;
    public int navIcon;
    public float opacity = 1;

    private Theme() {
    }

    public boolean isDark() {
        return Colors.red(windowBg) < 100;
    }

    public int fade(int color) {
        return Colors.reAlpha(color, Math.round(Colors.alpha(color) * opacity));
    }

    static Theme of(boolean light, int accent, float opacity) {
        Theme t = light ? light() : dark();
        t.opacity = opacity;
        t.windowBg = t.fade(t.windowBg);
        t.categoryPanelBg = t.fade(t.categoryPanelBg);
        t.modulePanelBg = t.fade(t.modulePanelBg);
        t.iconHighlight = accent;
        t.selectorElementHover = Colors.reAlpha(accent, 25);
        t.selectorElementActive = Colors.reAlpha(accent, 40);
        t.scrollbarThumbActive = accent;
        return t;
    }

    private static Theme dark() {
        Theme t = new Theme();
        t.clientIconBg = rgb(8, 27, 48);
        t.clientName = rgb(228, 230, 236);
        t.windowBg = rgba(13, 15, 22, 222);
        t.windowBorder = rgb(30, 33, 43);
        t.categoryPanelBg = rgba(18, 21, 30, 231);
        t.main = rgb(207, 209, 218);
        t.secondary = rgb(91, 96, 108);
        t.textHighlight = rgb(224, 229, 243);
        t.categoryHighlight = rgb(39, 43, 54);
        t.modulePanelBg = rgba(17, 19, 27, 224);
        t.split = rgb(28, 31, 40);
        t.switchDisabledBg = rgb(29, 33, 43);
        t.buttonBg = rgb(25, 28, 38);
        t.buttonHoveredBg = rgb(31, 38, 54);
        t.buttonActiveBg = rgb(39, 43, 54);
        t.sliderBg = rgb(34, 38, 48);
        t.selectorBg = rgb(25, 28, 38);
        t.scrollbarTrack = 0;
        t.scrollbarThumb = rgb(62, 70, 81);
        t.scrollbarThumbHover = rgb(98, 108, 121);
        t.navText = rgb(145, 149, 159);
        t.navIcon = rgb(137, 142, 153);
        return t;
    }

    private static Theme light() {
        Theme t = dark();
        t.clientIconBg = rgb(200, 201, 199);
        t.clientName = rgb(38, 38, 38);
        t.secondary = rgb(118, 120, 117);
        t.windowBg = rgba(218, 218, 216, 242);
        t.windowBorder = rgb(206, 208, 209);
        t.categoryPanelBg = rgba(182, 184, 180, 242);
        t.main = rgb(52, 52, 52);
        t.textHighlight = rgb(38, 39, 36);
        t.categoryHighlight = rgb(163, 163, 161);
        t.modulePanelBg = rgba(229, 229, 229, 245);
        t.split = rgb(210, 210, 210);
        t.switchDisabledBg = rgb(210, 210, 210);
        t.buttonBg = rgb(210, 210, 210);
        t.selectorBg = rgb(210, 210, 210);
        t.buttonHoveredBg = rgb(198, 198, 198);
        t.buttonActiveBg = rgb(188, 188, 188);
        t.sliderBg = rgb(200, 200, 200);
        t.scrollbarThumb = rgb(170, 170, 170);
        t.scrollbarThumbHover = rgb(140, 140, 140);
        t.navText = rgb(97, 100, 97);
        t.navIcon = rgb(99, 100, 96);
        return t;
    }
}
