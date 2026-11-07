package wtf.moonlight.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import wtf.moonlight.command.Command;
import wtf.moonlight.command.CommandManager;
import wtf.moonlight.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;

public final class HelpCommand extends Command {
    private final CommandManager manager;

    public HelpCommand(CommandManager manager) {
        super("help", "Lists all commands, or shows how to use one", "h");
        this.manager = manager;
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> root) {
        root.executes(context -> listCommands())
                .then(argument("command", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(commandNames(), builder))
                        .executes(context -> showUsage(StringArgumentType.getString(context, "command"), context.getSource())));
    }

    private int listCommands() {
        String prefix = manager.getPrefix();
        ChatUtils.info("Commands ({}):", manager.getCommands().size());
        for (Command command : manager.getCommands()) {
            MutableComponent line = Component.literal("  " + prefix + command.getName())
                    .withStyle(ChatFormatting.WHITE)
                    .withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand(prefix + command.getName() + " ")));
            if (!command.getAliases().isEmpty()) {
                line.append(Component.literal(" (" + String.join(", ", command.getAliases()) + ")").withStyle(ChatFormatting.DARK_GRAY));
            }
            line.append(Component.literal(" - " + command.getDescription()).withStyle(ChatFormatting.GRAY));
            ChatUtils.send(line);
        }
        return SUCCESS;
    }

    private int showUsage(String name, ClientSuggestionProvider source) {
        Command command = manager.getCommand(name);
        if (command == null) {
            manager.reportUnknown();
            return 0;
        }
        String base = manager.getPrefix() + command.getName();
        ChatUtils.info("{} {}", base, Component.literal("- " + command.getDescription()).withStyle(ChatFormatting.GRAY));
        CommandNode<ClientSuggestionProvider> node = manager.getDispatcher().getRoot().getChild(command.getName());
        if (node.getCommand() != null) {
            ChatUtils.send(usage(base, ""));
        }
        for (String usage : manager.getDispatcher().getSmartUsage(node, source).values()) {
            ChatUtils.send(usage(base, " " + usage));
        }
        return SUCCESS;
    }

    private static Component usage(String base, String arguments) {
        return Component.literal("  " + base).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(arguments).withStyle(ChatFormatting.GRAY));
    }

    private List<String> commandNames() {
        List<String> names = new ArrayList<>();
        for (Command command : manager.getCommands()) {
            names.add(command.getName());
        }
        return names;
    }
}
