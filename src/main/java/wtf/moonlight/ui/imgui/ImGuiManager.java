package wtf.moonlight.ui.imgui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import imgui.ImDrawData;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.render.RenderOverlayEvent;

public final class ImGuiManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("moonlight/imgui");

    private static boolean ready;
    private static long styleBackup;
    private static float displayScale = 1;

    private final Runnable frame;
    private @Nullable ImGuiRenderer renderer;
    private boolean failed;
    private boolean renderFailed;
    private boolean verified;
    private long lastTime;

    public ImGuiManager(Runnable frame) {
        this.frame = frame;
    }

    public static boolean isReady() {
        return ready;
    }

    public static float displayScale() {
        return displayScale;
    }

    public static void saveStyle() {
        Natives.copyStyle(ImGui.getStyle().ptr, styleBackup);
    }

    public static void restoreStyle() {
        Natives.copyStyle(styleBackup, ImGui.getStyle().ptr);
    }

    @Subscribe
    private void onRender(RenderOverlayEvent event) {
        if (failed) return;
        try {
            if (renderer == null) init();
            frame();
        } catch (RuntimeException exception) {
            failed = true;
            ready = false;
            LOGGER.error("ImGui stopped after an error", exception);
        }
    }

    private void init() {
        setup(SDLVideo.SDL_GetWindowDisplayScale(Minecraft.getInstance().getWindow().handle()));
        renderer = new ImGuiRenderer();
        ready = true;
    }

    static void setup(float scale) {
        displayScale = scale > 0 ? scale : 1;
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.setConfigErrorRecoveryEnableAssert(false);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard | ImGuiConfigFlags.NavEnableGamepad);
        io.addBackendFlags(ImGuiBackendFlags.RendererHasTextures | ImGuiBackendFlags.RendererHasVtxOffset | ImGuiBackendFlags.HasMouseCursors);
        Fonts.load(io.getFonts());
        ImGui.styleColorsDark();
        ImGuiStyle style = ImGui.getStyle();
        style.setColor(ImGuiCol.NavCursor, 0, 0, 0, 0);
        if (!Natives.matchesStyle(style)) throw new IllegalStateException("Unexpected ImGuiStyle layout");
        styleBackup = Natives.allocateStyle();
    }

    private void frame() {
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(target.width, target.height);
        long now = System.nanoTime();
        io.setDeltaTime(lastTime == 0 ? 1.0F / 60.0F : Math.max((now - lastTime) / 1.0e9F, 1.0e-6F));
        lastTime = now;
        ImGui.newFrame();
        frame.run();
        ImGui.render();
        ImDrawData data = ImGui.getDrawData();
        if (!verified && Natives.totalVertices(data) > 0) {
            if (!Natives.matchesDrawData(data)) throw new IllegalStateException("Unexpected ImDrawData layout");
            verified = true;
        }
        if (renderer == null) return;
        try {
            renderer.render(data);
        } catch (RuntimeException exception) {
            if (!renderFailed) LOGGER.error("ImGui could not draw a frame", exception);
            renderFailed = true;
        }
    }
}
