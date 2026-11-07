package wtf.moonlight.ui.clickgui;

import static wtf.moonlight.ui.clickgui.Colors.rgb;
import static wtf.moonlight.ui.clickgui.Colors.rgba;

public final class Theme {
    public static final Theme DARK = dark();
    public static final Theme LIGHT = light();

    public int clientIconBg;
    public int clientIcon;
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

    private Theme() {
    }

    public boolean isDark() {
        return Colors.red(windowBg) < 100;
    }

    private static Theme dark() {
        Theme t = new Theme();
        t.clientIconBg = rgb(8, 27, 48);
        t.clientIcon = rgb(94, 185, 255);
        t.clientName = rgb(228, 230, 236);
        t.windowBg = rgba(13, 15, 22, 222);
        t.windowBorder = rgb(30, 33, 43);
        t.categoryPanelBg = rgba(18, 21, 30, 231);
        t.main = rgb(207, 209, 218);
        t.secondary = rgb(91, 96, 108);
        t.textHighlight = rgb(224, 229, 243);
        t.iconHighlight = rgb(75, 126, 255);
        t.categoryHighlight = rgb(39, 43, 54);
        t.modulePanelBg = rgba(17, 19, 27, 224);
        t.split = rgb(28, 31, 40);
        t.switchDisabledBg = rgb(29, 33, 43);
        t.buttonBg = rgb(25, 28, 38);
        t.buttonHoveredBg = rgb(31, 38, 54);
        t.buttonActiveBg = rgb(39, 43, 54);
        t.sliderBg = rgb(34, 38, 48);
        t.selectorBg = rgb(25, 28, 38);
        t.selectorElementHover = rgba(75, 126, 255, 25);
        t.selectorElementActive = rgba(75, 126, 255, 40);
        t.scrollbarTrack = 0;
        t.scrollbarThumb = rgb(62, 70, 81);
        t.scrollbarThumbHover = rgb(98, 108, 121);
        t.scrollbarThumbActive = rgb(75, 126, 255);
        return t;
    }

    private static Theme light() {
        Theme t = dark();
        t.clientIconBg = rgb(222, 234, 251);
        t.clientName = rgb(33, 39, 53);
        t.secondary = rgb(107, 115, 132);
        t.windowBg = rgba(242, 244, 249, 240);
        t.windowBorder = rgb(205, 211, 224);
        t.categoryPanelBg = rgba(232, 236, 245, 240);
        t.main = rgb(48, 56, 73);
        t.textHighlight = rgb(38, 75, 148);
        t.categoryHighlight = rgb(215, 225, 244);
        t.modulePanelBg = rgba(250, 251, 254, 230);
        t.split = rgb(222, 227, 237);
        t.switchDisabledBg = rgb(216, 223, 234);
        t.buttonBg = rgb(232, 236, 245);
        t.selectorBg = rgb(232, 236, 245);
        t.buttonHoveredBg = rgb(221, 229, 246);
        t.buttonActiveBg = rgb(209, 222, 246);
        t.sliderBg = rgb(211, 219, 234);
        t.scrollbarThumb = rgb(163, 174, 194);
        t.scrollbarThumbHover = rgb(121, 140, 171);
        return t;
    }
}
