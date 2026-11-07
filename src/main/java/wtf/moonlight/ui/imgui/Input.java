package wtf.moonlight.ui.imgui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.sdl3.ImGuiImplSdl3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.sdl.SDLKeycode;

public final class Input {
    private Input() {
    }

    public static void focus(boolean focused) {
        if (ImGuiManager.isReady()) ImGui.getIO().addFocusEvent(focused);
    }

    public static void mouseMoved() {
        if (!ImGuiManager.isReady()) return;
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        double scaleX = (double) window.getWidth() / Math.max(1, window.getScreenWidth());
        double scaleY = (double) window.getHeight() / Math.max(1, window.getScreenHeight());
        ImGui.getIO().addMousePosEvent((float) (minecraft.mouseHandler.xpos() * scaleX), (float) (minecraft.mouseHandler.ypos() * scaleY));
    }

    public static void mouseLeft() {
        if (ImGuiManager.isReady()) ImGui.getIO().addMousePosEvent(-Float.MAX_VALUE, -Float.MAX_VALUE);
    }

    public static void mouseButton(int button, boolean down) {
        if (!ImGuiManager.isReady()) return;
        int imgui = switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT -> ImGuiMouseButton.Left;
            case InputConstants.MOUSE_BUTTON_RIGHT -> ImGuiMouseButton.Right;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> ImGuiMouseButton.Middle;
            case InputConstants.MOUSE_BUTTON_4 -> 3;
            case InputConstants.MOUSE_BUTTON_5 -> 4;
            default -> -1;
        };
        if (imgui >= 0) ImGui.getIO().addMouseButtonEvent(imgui, down);
    }

    public static void scroll(double x, double y) {
        if (ImGuiManager.isReady()) ImGui.getIO().addMouseWheelEvent((float) -x, (float) y);
    }

    public static void key(KeyEvent event, boolean down) {
        if (!ImGuiManager.isReady()) return;
        ImGuiIO io = ImGui.getIO();
        int mods = event.modifiers();
        io.addKeyEvent(ImGuiKey.ImGuiMod_Ctrl, (mods & SDLKeycode.SDL_KMOD_CTRL) != 0);
        io.addKeyEvent(ImGuiKey.ImGuiMod_Shift, (mods & SDLKeycode.SDL_KMOD_SHIFT) != 0);
        io.addKeyEvent(ImGuiKey.ImGuiMod_Alt, (mods & SDLKeycode.SDL_KMOD_ALT) != 0);
        io.addKeyEvent(ImGuiKey.ImGuiMod_Super, (mods & SDLKeycode.SDL_KMOD_GUI) != 0);
        int key = ImGuiImplSdl3.keyEventToImGuiKey(event.keycode(), event.key());
        if (key != ImGuiKey.None) io.addKeyEvent(key, down);
    }

    public static void character(int codepoint) {
        if (ImGuiManager.isReady()) ImGui.getIO().addInputCharacter(codepoint);
    }

    public static boolean wantsText() {
        return ImGuiManager.isReady() && ImGui.getIO().getWantTextInput();
    }

    public static int cursor() {
        return ImGuiManager.isReady() ? ImGui.getMouseCursor() : -1;
    }
}
