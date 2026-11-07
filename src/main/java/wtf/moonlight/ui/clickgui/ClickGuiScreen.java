package wtf.moonlight.ui.clickgui;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import imgui.flag.ImGuiMouseCursor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import wtf.moonlight.ui.imgui.Input;

import java.util.function.Consumer;

public final class ClickGuiScreen extends Screen {
    private final Panel panel;
    private final Consumer<ClickGuiScreen> onClosed;

    public ClickGuiScreen(Panel panel, Consumer<ClickGuiScreen> onClosed) {
        super(Component.literal("ClickGUI"));
        this.panel = panel;
        this.onClosed = onClosed;
    }

    @Override
    protected void init() {
        Input.focus(true);
        Input.mouseMoved();
    }

    @Override
    public void removed() {
        minecraft.textInputManager().stopTextInput(this);
        Input.focus(false);
        Input.mouseLeft();
        onClosed.accept(this);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (Input.wantsText()) minecraft.textInputManager().startTextInput(this);
        else minecraft.textInputManager().stopTextInput(this);
        graphics.requestCursor(cursor(Input.cursor()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            panel.close();
            onClose();
            return true;
        }
        Input.key(event, true);
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        Input.key(event, false);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        Input.character(event.codepoint());
        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        Input.mouseMoved();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Input.mouseMoved();
        Input.mouseButton(event.button(), true);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        Input.mouseMoved();
        Input.mouseButton(event.button(), false);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        Input.mouseMoved();
        Input.scroll(scrollX, scrollY);
        return true;
    }

    private static CursorType cursor(int cursor) {
        return switch (cursor) {
            case ImGuiMouseCursor.TextInput -> CursorTypes.IBEAM;
            case ImGuiMouseCursor.ResizeAll -> CursorTypes.RESIZE_ALL;
            case ImGuiMouseCursor.ResizeNS -> CursorTypes.RESIZE_NS;
            case ImGuiMouseCursor.ResizeEW -> CursorTypes.RESIZE_EW;
            case ImGuiMouseCursor.Hand -> CursorTypes.POINTING_HAND;
            case ImGuiMouseCursor.NotAllowed -> CursorTypes.NOT_ALLOWED;
            default -> CursorTypes.ARROW;
        };
    }
}
