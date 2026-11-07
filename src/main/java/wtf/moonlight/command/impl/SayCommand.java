package wtf.moonlight.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import wtf.moonlight.command.Command;
import wtf.moonlight.command.CommandManager;

public final class SayCommand extends Command {
    private final CommandManager manager;

    public SayCommand(CommandManager manager) {
        super("say", "Sends a chat message as-is, even if it starts with the command prefix");
        this.manager = manager;
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> root) {
        root.then(argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    manager.sendChat(StringArgumentType.getString(context, "message"));
                    return SUCCESS;
                }));
    }
}
