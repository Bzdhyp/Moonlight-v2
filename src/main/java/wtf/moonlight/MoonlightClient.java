package wtf.moonlight;

import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.bind.BindCapture;
import wtf.moonlight.bind.BindManager;
import wtf.moonlight.command.CommandManager;
import wtf.moonlight.command.impl.BindCommand;
import wtf.moonlight.command.impl.ConfigCommand;
import wtf.moonlight.command.impl.ToggleCommand;
import wtf.moonlight.config.ConfigManager;
import wtf.moonlight.module.ModuleManager;
import wtf.moonlight.module.impl.render.PostProcess;
import wtf.moonlight.platform.Platform;
import wtf.moonlight.rotation.RotationManager;
import wtf.moonlight.ui.clickgui.ClickGuiScreen;
import wtf.moonlight.ui.clickgui.ClientSettings;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.imgui.Effects;
import wtf.moonlight.ui.imgui.ImGuiManager;

import java.nio.file.Path;

public final class MoonlightClient {
    private static final RotationManager ROTATIONS = new RotationManager();
    private static @Nullable CommandManager commands;
    private static @Nullable Panel panel;

    private MoonlightClient() {
    }

    public static void init() {
        Path configDir = Platform.INSTANCE.configDir().resolve(Moonlight.MOD_ID);
        BindManager binds = new BindManager();
        BindCapture capture = new BindCapture();
        ModuleManager modules = new ModuleManager(binds);
        Effects.use(modules.getModule(PostProcess.class));
        ClientSettings settings = new ClientSettings();
        ConfigManager configs = new ConfigManager(configDir.resolve("configs"), modules, settings.getValues());
        commands = new CommandManager(configDir.resolve("commands.properties"));
        commands.register(new BindCommand(binds, capture), new ToggleCommand(modules), new ConfigCommand(configs));
        Panel clickGui = new Panel(modules, configs, settings, capture, () -> Minecraft.getInstance().getUser().getName());
        panel = clickGui;

        Moonlight.EVENT_BUS.register(ROTATIONS);
        Moonlight.EVENT_BUS.register(capture);
        Moonlight.EVENT_BUS.register(binds);
        Moonlight.EVENT_BUS.register(configs);
        Moonlight.EVENT_BUS.register(commands);
        Moonlight.EVENT_BUS.register(new ImGuiManager(() -> clickGui.frame(Minecraft.getInstance().gui.screen() instanceof ClickGuiScreen)));
        Moonlight.LOGGER.info("Moonlight client ready");
    }

    public static RotationManager getRotations() {
        return ROTATIONS;
    }

    public static @Nullable CommandManager getCommands() {
        return commands;
    }

    public static @Nullable Panel getPanel() {
        return panel;
    }
}
