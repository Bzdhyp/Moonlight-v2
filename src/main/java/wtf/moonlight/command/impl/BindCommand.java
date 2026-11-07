package wtf.moonlight.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.bind.Bind;
import wtf.moonlight.bind.BindCapture;
import wtf.moonlight.bind.BindManager;
import wtf.moonlight.bind.Hotkey;
import wtf.moonlight.command.Command;
import wtf.moonlight.utils.ChatUtils;
import wtf.moonlight.utils.KeyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BindCommand extends Command {
    private final BindManager binds;
    private final BindCapture capture;

    public BindCommand(BindManager binds, BindCapture capture) {
        super("bind", "Lists binds, or sets one: <name> [key] [toggle|hold]", "b");
        this.binds = binds;
        this.capture = capture;
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> root) {
        root.executes(context -> list())
                .then(argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(bindNames(), builder))
                        .executes(context -> capture(StringArgumentType.getString(context, "name")))
                        .then(argument("key", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(KeyUtils.suggestions(), builder))
                                .executes(context -> set(
                                        StringArgumentType.getString(context, "name"),
                                        StringArgumentType.getString(context, "key"),
                                        null))
                                .then(argument("mode", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("toggle", "hold"), builder))
                                        .executes(context -> set(
                                                StringArgumentType.getString(context, "name"),
                                                StringArgumentType.getString(context, "key"),
                                                StringArgumentType.getString(context, "mode"))))));
    }

    private int list() {
        if (binds.getBinds().isEmpty()) {
            ChatUtils.info("No binds registered.");
            return SUCCESS;
        }
        ChatUtils.info("Binds ({}):", binds.getBinds().size());
        for (Bind bind : binds.getBinds()) {
            ChatUtils.send(Component.literal("  " + bind.getName()).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(keys(bind)).withStyle(bind.isBound() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY)));
        }
        return SUCCESS;
    }

    private int capture(String name) {
        Bind bind = find(name);
        if (bind == null) return 0;
        Hotkey hotkey = bind.primary();
        capture.begin(hotkey, () -> {
            if (hotkey.getKey() == KeyUtils.NONE) bind.remove(hotkey);
            report(bind);
        }, () -> {
            if (hotkey.getKey() == KeyUtils.NONE) bind.remove(hotkey);
            ChatUtils.info("Stopped binding {}.", bind.getName());
        });
        ChatUtils.info("Press a key to bind {}. {} clears the bind, {} cancels.", bind.getName(), "Esc", "Left click");
        return SUCCESS;
    }

    private int set(String name, String keyName, @Nullable String mode) {
        Bind bind = find(name);
        if (bind == null) return 0;
        Integer code = KeyUtils.parse(keyName);
        if (code == null) {
            ChatUtils.error("Unknown key: {}", keyName);
            return 0;
        }
        Boolean hold = null;
        if (mode != null) {
            switch (mode.toLowerCase(Locale.ROOT)) {
                case "hold" -> hold = true;
                case "toggle" -> hold = false;
                default -> {
                    ChatUtils.error("Unknown mode: {} (use {} or {})", mode, "toggle", "hold");
                    return 0;
                }
            }
        }
        if (code == KeyUtils.NONE) {
            bind.clear();
        } else {
            Hotkey hotkey = bind.primary();
            hotkey.setKey(code);
            if (hold != null) hotkey.setHold(hold);
        }
        report(bind);
        return SUCCESS;
    }

    private @Nullable Bind find(String name) {
        Bind bind = binds.getBind(name);
        if (bind == null) ChatUtils.error("Unknown bind: {}", name);
        return bind;
    }

    private List<String> bindNames() {
        List<String> names = new ArrayList<>();
        for (Bind bind : binds.getBinds()) {
            names.add(bind.getName().replace(" ", ""));
        }
        return names;
    }

    private static void report(Bind bind) {
        if (!bind.isBound()) {
            ChatUtils.info("{} is no longer bound.", bind.getName());
            return;
        }
        ChatUtils.info("{} bound to {}.", bind.getName(), keys(bind));
    }

    private static String keys(Bind bind) {
        List<String> keys = new ArrayList<>();
        for (Hotkey hotkey : bind.getHotkeys()) {
            if (hotkey.getKey() != KeyUtils.NONE) keys.add(KeyUtils.getName(hotkey.getKey()) + (hotkey.isHold() ? " (hold)" : ""));
        }
        return keys.isEmpty() ? KeyUtils.getName(KeyUtils.NONE) : String.join(", ", keys);
    }
}
