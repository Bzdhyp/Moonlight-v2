package wtf.moonlight.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.moonlight.command.impl.HelpCommand;
import wtf.moonlight.command.impl.PrefixCommand;
import wtf.moonlight.command.impl.SayCommand;
import wtf.moonlight.event.Priority;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.network.ChatSendEvent;
import wtf.moonlight.utils.ChatUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class CommandManager {
    public static final String DEFAULT_PREFIX = ".";
    private static final Logger LOGGER = LoggerFactory.getLogger("moonlight/commands");

    private final CommandDispatcher<ClientSuggestionProvider> dispatcher = new CommandDispatcher<>();
    private final List<Command> commands = new ArrayList<>();
    private final Path settingsFile;
    private String prefix = DEFAULT_PREFIX;
    private boolean bypassChat;

    public CommandManager(Path settingsFile) {
        this.settingsFile = settingsFile;
        loadSettings();
        register(new HelpCommand(this), new PrefixCommand(this), new SayCommand(this));
    }

    public void register(Command... added) {
        for (Command command : added) {
            commands.add(command);
            registerLiteral(command, command.getName());
            for (String alias : command.getAliases()) {
                registerLiteral(command, alias);
            }
        }
    }

    private void registerLiteral(Command command, String literal) {
        LiteralArgumentBuilder<ClientSuggestionProvider> root = LiteralArgumentBuilder.literal(literal);
        command.build(root);
        dispatcher.register(root);
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public @Nullable Command getCommand(String name) {
        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(name)) return command;
            for (String alias : command.getAliases()) {
                if (alias.equalsIgnoreCase(name)) return command;
            }
        }
        return null;
    }

    public CommandDispatcher<ClientSuggestionProvider> getDispatcher() {
        return dispatcher;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean setPrefix(String prefix) {
        if (!isValidPrefix(prefix)) return false;
        this.prefix = prefix;
        saveSettings();
        return true;
    }

    private static boolean isValidPrefix(String prefix) {
        return !prefix.isEmpty() && prefix.length() <= 3 && !prefix.startsWith("/") && prefix.chars().noneMatch(Character::isWhitespace);
    }

    @Subscribe(priority = Priority.HIGHEST)
    private void onChat(ChatSendEvent event) {
        if (bypassChat || !event.getMessage().startsWith(prefix)) return;
        event.cancel();
        execute(event.getMessage().substring(prefix.length()));
    }

    private void execute(String input) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        try {
            dispatcher.execute(input, connection.getSuggestionsProvider());
        } catch (CommandSyntaxException exception) {
            reportSyntaxError(input, exception);
        } catch (RuntimeException exception) {
            LOGGER.error("Command '{}' failed", input, exception);
            ChatUtils.error("The command failed: {}", exception.getMessage());
        }
    }

    public void sendChat(String message) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        bypassChat = true;
        try {
            connection.sendChat(message);
        } finally {
            bypassChat = false;
        }
    }

    private void reportSyntaxError(String input, CommandSyntaxException exception) {
        String name = input.split(" ", 2)[0];
        if (getCommand(name) == null) {
            reportUnknown();
            return;
        }
        ChatUtils.error(ComponentUtils.fromMessage(exception.getRawMessage()));
        if (exception.getInput() == null || exception.getCursor() < 0) return;
        String text = exception.getInput();
        int cursor = Math.min(text.length(), exception.getCursor());
        MutableComponent context = Component.empty()
                .withStyle(ChatFormatting.GRAY)
                .withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand(prefix + input)));
        if (cursor > 10) context.append(CommonComponents.ELLIPSIS);
        context.append(text.substring(Math.max(0, cursor - 10), cursor));
        if (cursor < text.length()) {
            context.append(Component.literal(text.substring(cursor)).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
        }
        context.append(Component.literal("<--[HERE]").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        ChatUtils.send(context);
    }

    public void reportUnknown() {
        ChatUtils.error("Unknown command. Type {} for a list of commands.", prefix + "help");
    }

    private void loadSettings() {
        if (!Files.isRegularFile(settingsFile)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            LOGGER.warn("Could not read {}", settingsFile, exception);
            return;
        }
        String saved = properties.getProperty("prefix", DEFAULT_PREFIX);
        if (isValidPrefix(saved)) {
            prefix = saved;
        } else {
            LOGGER.warn("Ignoring invalid command prefix '{}' in {}", saved, settingsFile);
        }
    }

    private void saveSettings() {
        Properties properties = new Properties();
        properties.setProperty("prefix", prefix);
        try {
            Files.createDirectories(settingsFile.getParent());
            try (Writer writer = Files.newBufferedWriter(settingsFile, StandardCharsets.UTF_8)) {
                properties.store(writer, null);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save {}", settingsFile, exception);
        }
    }
}
