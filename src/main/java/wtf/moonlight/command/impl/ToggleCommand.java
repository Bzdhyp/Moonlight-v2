package wtf.moonlight.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import wtf.moonlight.command.Command;
import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.ModuleManager;
import wtf.moonlight.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;

public final class ToggleCommand extends Command {
    private final ModuleManager modules;

    public ToggleCommand(ModuleManager modules) {
        super("toggle", "Lists modules, or toggles one: <module>", "t");
        this.modules = modules;
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> root) {
        root.executes(context -> list())
                .then(argument("module", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(moduleNames(), builder))
                        .executes(context -> toggle(StringArgumentType.getString(context, "module"))));
    }

    private int list() {
        ChatUtils.info("Modules ({}):", modules.getModules().size());
        for (Category category : Category.values()) {
            MutableComponent line = Component.literal("  " + category.getName() + ":").withStyle(ChatFormatting.GRAY);
            boolean empty = true;
            for (Module module : modules.getModules()) {
                if (module.getCategory() != category) continue;
                empty = false;
                line.append(" ").append(Component.literal(module.getName())
                        .withStyle(module.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            }
            if (!empty) ChatUtils.send(line);
        }
        return SUCCESS;
    }

    private int toggle(String name) {
        Module module = modules.getModule(name);
        if (module == null) {
            ChatUtils.error("Unknown module: {}", name);
            return 0;
        }
        module.setEnabled(!module.isEnabled());
        Component state = module.isEnabled()
                ? Component.literal("enabled").withStyle(ChatFormatting.GREEN)
                : Component.literal("disabled").withStyle(ChatFormatting.RED);
        ChatUtils.info("{} {}", module.getName(), state);
        return SUCCESS;
    }

    private List<String> moduleNames() {
        List<String> names = new ArrayList<>();
        for (Module module : modules.getModules()) {
            names.add(module.getName().replace(" ", ""));
        }
        return names;
    }
}
