package wtf.moonlight.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import wtf.moonlight.command.Command;
import wtf.moonlight.command.CommandManager;
import wtf.moonlight.utils.ChatUtils;

public final class PrefixCommand extends Command {
    private final CommandManager manager;

    public PrefixCommand(CommandManager manager) {
        super("prefix", "Shows or changes the command prefix");
        this.manager = manager;
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> root) {
        root.executes(context -> {
                    ChatUtils.info("Current command prefix: {}", manager.getPrefix());
                    return SUCCESS;
                })
                .then(argument("prefix", StringArgumentType.greedyString())
                        .executes(context -> {
                            String prefix = StringArgumentType.getString(context, "prefix").trim();
                            if (!manager.setPrefix(prefix)) {
                                ChatUtils.error("A prefix must be {} characters, without spaces, and must not start with {}", "1-3", "/");
                                return 0;
                            }
                            ChatUtils.info("Command prefix changed to {}", prefix);
                            return SUCCESS;
                        }));
    }
}
