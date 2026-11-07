package wtf.moonlight.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.List;

public abstract class Command {
    public static final int SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;

    private final String name;
    private final String description;
    private final List<String> aliases;

    protected Command(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = List.of(aliases);
    }

    public abstract void build(LiteralArgumentBuilder<ClientSuggestionProvider> root);

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getDescription() {
        return description;
    }

    protected static LiteralArgumentBuilder<ClientSuggestionProvider> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    protected static <T> RequiredArgumentBuilder<ClientSuggestionProvider, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
