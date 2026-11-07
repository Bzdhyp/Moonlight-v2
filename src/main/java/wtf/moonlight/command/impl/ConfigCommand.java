package wtf.moonlight.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import wtf.moonlight.command.Command;
import wtf.moonlight.config.ConfigManager;
import wtf.moonlight.utils.ChatUtils;

import java.util.List;

public final class ConfigCommand extends Command {
    private final ConfigManager configs;

    public ConfigCommand(ConfigManager configs) {
        super("config", "Lists, saves or loads configs: [save|load] [name]", "cfg");
        this.configs = configs;
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> root) {
        root.executes(context -> list())
                .then(literal("save")
                        .executes(context -> save(ConfigManager.LATEST))
                        .then(name().executes(context -> save(StringArgumentType.getString(context, "name")))))
                .then(literal("load")
                        .then(name().executes(context -> load(StringArgumentType.getString(context, "name")))));
    }

    private RequiredArgumentBuilder<ClientSuggestionProvider, String> name() {
        return argument("name", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    configs.refresh();
                    return SharedSuggestionProvider.suggest(configs.getConfigs(), builder);
                });
    }

    private int list() {
        configs.refresh();
        List<String> names = configs.getConfigs();
        if (names.isEmpty()) {
            ChatUtils.info("No configs saved yet.");
            return SUCCESS;
        }
        ChatUtils.info("Configs ({}):", names.size());
        for (String name : names) {
            ChatUtils.send(Component.literal("  " + name).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(name.equals(ConfigManager.LATEST) ? " (auto)" : "").withStyle(ChatFormatting.DARK_GRAY)));
        }
        return SUCCESS;
    }

    private int save(String name) {
        if (!ConfigManager.isValidName(name)) return invalid(name);
        if (!configs.save(name)) {
            ChatUtils.error("Could not save config {}.", name);
            return 0;
        }
        ChatUtils.info("Saved config {}.", name);
        return SUCCESS;
    }

    private int load(String name) {
        if (!ConfigManager.isValidName(name)) return invalid(name);
        configs.refresh();
        if (!configs.load(name)) {
            ChatUtils.error("Could not load config {}.", name);
            return 0;
        }
        ChatUtils.info("Loaded config {}.", name);
        return SUCCESS;
    }

    private static int invalid(String name) {
        ChatUtils.error("Invalid config name: {} (use letters, numbers, spaces, {}, {} or {})", name, "_", "-", ".");
        return 0;
    }
}
