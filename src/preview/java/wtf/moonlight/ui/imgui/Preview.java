package wtf.moonlight.ui.imgui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiKey;
import imgui.sdl3.ImGuiImplSdl3;
import net.minecraft.SharedConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.moonlight.Moonlight;
import wtf.moonlight.bind.BindCapture;
import wtf.moonlight.bind.BindManager;
import wtf.moonlight.config.ConfigManager;
import wtf.moonlight.event.EventBus;
import wtf.moonlight.event.impl.client.GameLoadedEvent;
import wtf.moonlight.event.impl.client.TickEvent;
import wtf.moonlight.event.impl.input.KeyInputEvent;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.ModuleManager;
import wtf.moonlight.module.impl.render.Hotkeys;
import wtf.moonlight.module.impl.render.PostProcess;
import wtf.moonlight.module.impl.render.Watermark;
import wtf.moonlight.ui.clickgui.ClientSettings;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.hud.HotkeysRenderer;
import wtf.moonlight.ui.hud.Hud;
import wtf.moonlight.ui.hud.Snapshot;
import wtf.moonlight.ui.hud.WatermarkRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLTimer.SDL_Delay;
import static org.lwjgl.sdl.SDLVideo.*;

public final class Preview {
    private static final Logger LOGGER = LoggerFactory.getLogger("moonlight/preview");
    private static final Identifier SKIN = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final Identifier PANORAMA = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_0.png");
    private static final String USER = "Steve";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final float FILL = 0.8F;
    private static final long TICK_NANOS = 50_000_000L;

    private Preview() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Map<String, Path> objects = objects();
        Function<Identifier, BufferedImage> images = path -> image(objects, path);
        if (!SDL_Init(SDL_INIT_VIDEO)) throw new IllegalStateException(SDL_GetError());
        int windowWidth = WIDTH;
        int windowHeight = HEIGHT;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            if (SDL_GetDisplayUsableBounds(SDL_GetPrimaryDisplay(), bounds)) {
                windowWidth = Math.round(bounds.w() * FILL);
                windowHeight = Math.round(bounds.h() * FILL);
            }
        }
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG);
        long window = SDL_CreateWindow("Moonlight UI Preview", windowWidth, windowHeight, SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE);
        if (window == 0) throw new IllegalStateException(SDL_GetError());
        long context = SDL_GL_CreateContext(window);
        if (context == 0) throw new IllegalStateException(SDL_GetError());
        SDL_GL_MakeCurrent(window, context);
        SDL_GL_SetSwapInterval(1);
        GL.createCapabilities();

        ImGuiManager.setup(SDL_GetWindowDisplayScale(window));
        ImGuiImplSdl3 platform = new ImGuiImplSdl3();
        platform.initForOpenGL(window, context);
        PreviewRenderer renderer = new PreviewRenderer(images, backdrop(images));

        BindManager binds = new BindManager();
        BindCapture capture = new BindCapture();
        ModuleManager modules = new ModuleManager(binds);
        for (Module module : modules.getModules()) module.getBind().setGate(() -> !ImGui.getIO().getWantTextInput());
        Moonlight.EVENT_BUS.register(binds);
        Moonlight.EVENT_BUS.register(capture);
        PostProcess post = modules.getModule(PostProcess.class);
        Watermark watermark = modules.getModule(Watermark.class);
        Hotkeys hotkeys = modules.getModule(Hotkeys.class);
        Effects.use(post);
        post.setEnabled(true);
        watermark.setEnabled(true);
        hotkeys.setEnabled(true);
        ClientSettings settings = new ClientSettings();
        ConfigManager configs = new ConfigManager(Path.of("preview", "configs"), modules, settings.getValues());
        EventBus bus = new EventBus();
        bus.register(configs);
        bus.post(GameLoadedEvent.INSTANCE);
        Supplier<String> user = () -> USER;
        Supplier<Identifier> skin = () -> SKIN;
        Panel panel = new Panel(modules, configs, settings, capture, user, skin);
        String version = SharedConstants.getCurrentVersion().name();
        Hud hud = new Hud(List.of(new WatermarkRenderer(watermark, user, skin, () -> snapshot(version)), new HotkeysRenderer(hotkeys, modules)),
                panel, settings, () -> false);

        long style = Natives.allocateStyle();
        long tick = System.nanoTime();
        boolean open = true;
        boolean running = true;
        SDL_Event event = SDL_Event.malloc();
        while (running) {
            while (SDL_PollEvent(event)) {
                platform.processEvent(event.address());
                int type = event.type();
                if (type == SDL_EVENT_QUIT || type == SDL_EVENT_WINDOW_CLOSE_REQUESTED) running = false;
                if ((type == SDL_EVENT_KEY_DOWN || type == SDL_EVENT_KEY_UP) && !ImGui.getIO().getWantTextInput()) {
                    SDL_KeyboardEvent key = event.key();
                    int action = type == SDL_EVENT_KEY_UP ? InputConstants.RELEASE : key.repeat() ? InputConstants.REPEAT : InputConstants.PRESS;
                    Moonlight.EVENT_BUS.post(new KeyInputEvent(new KeyEvent(key.scancode(), key.key(), key.mod()), action));
                }
            }
            if (System.nanoTime() - tick >= TICK_NANOS) {
                tick = System.nanoTime();
                bus.post(TickEvent.PRE);
            }
            int width;
            int height;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                SDL_GetWindowSizeInPixels(window, w, h);
                width = w.get(0);
                height = h.get(0);
            }
            if (width <= 0 || height <= 0) {
                SDL_Delay(50);
                continue;
            }
            platform.newFrame();
            ImGuiIO io = ImGui.getIO();
            io.setDisplaySize(width, height);
            io.setDisplayFramebufferScale(1, 1);
            ImGui.newFrame();
            if (ImGui.isKeyPressed(ImGuiKey.RightShift, false) && !io.getWantTextInput()) open = !open;
            Natives.copyStyle(ImGui.getStyle().ptr, style);
            try {
                hud.frame(true, true);
                panel.frame(open, true);
            } catch (RuntimeException exception) {
                Natives.copyStyle(style, ImGui.getStyle().ptr);
                LOGGER.warn("Preview frame failed, usually a module that needs a running game", exception);
            }
            ImGui.render();
            renderer.render(ImGui.getDrawData(), width, height);
            SDL_GL_SwapWindow(window);
        }
        event.free();
        platform.shutdown();
        ImGui.destroyContext();
        SDL_GL_DestroyContext(context);
        SDL_DestroyWindow(window);
        SDL_Quit();
    }

    private static Snapshot snapshot(String version) {
        double time = ImGui.getTime();
        return new Snapshot("localhost", 24 + (int) Math.round(6 * Math.sin(time * 0.7)), 20, "128, 64, -256",
                4.3F + (float) Math.sin(time * 0.9), version);
    }

    private static @Nullable BufferedImage backdrop(Function<Identifier, BufferedImage> images) {
        try {
            BufferedImage image = images.apply(PANORAMA);
            return image.getWidth() > 1 ? image : null;
        } catch (UncheckedIOException exception) {
            LOGGER.warn("No backdrop available", exception);
            return null;
        }
    }

    private static BufferedImage image(Map<String, Path> objects, Identifier path) {
        String key = path.getNamespace() + "/" + path.getPath();
        try (InputStream stream = objects.containsKey(key) ? Files.newInputStream(objects.get(key))
                : Preview.class.getResourceAsStream("/assets/" + key)) {
            BufferedImage image = stream != null ? ImageIO.read(stream) : null;
            if (image == null) throw new IOException("Missing " + key);
            return image;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Map<String, Path> objects() {
        String dir = System.getProperty("moonlight.assets");
        if (dir == null) return Map.of();
        Path root = Path.of(dir);
        String prefix = SharedConstants.getCurrentVersion().name() + "-";
        try (Stream<Path> files = Files.list(root.resolve("indexes"))) {
            Optional<Path> index = files.filter(file -> file.getFileName().toString().startsWith(prefix)).max(Comparator.naturalOrder());
            if (index.isEmpty()) return Map.of();
            JsonObject entries = JsonParser.parseString(Files.readString(index.get())).getAsJsonObject().getAsJsonObject("objects");
            Map<String, Path> out = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
                String hash = entry.getValue().getAsJsonObject().get("hash").getAsString();
                out.put(entry.getKey(), root.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash));
            }
            return out;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read the asset index in {}", root, exception);
            return Map.of();
        }
    }
}
