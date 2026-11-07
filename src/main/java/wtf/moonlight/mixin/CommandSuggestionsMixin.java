package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.moonlight.MoonlightClient;
import wtf.moonlight.command.CommandManager;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow
    @Final
    private Screen screen;

    @Shadow
    @Final
    private EditBox input;

    @Shadow
    @Final
    private boolean onlyShowIfCursorPastError;

    @Shadow
    private ParseResults<ClientSuggestionProvider> currentParse;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;

    @Shadow
    private boolean keepSuggestions;

    @Shadow
    private boolean currentParseIsCommand;

    @Shadow
    private boolean currentParseIsMessage;

    @Shadow
    private void updateUsageInfo(ParseResults<ClientSuggestionProvider> parse, Suggestions result) {
    }

    @Inject(method = "updateCommandInfo", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;canRead()Z", ordinal = 0), cancellable = true)
    private void moonlight$clientCommands(CallbackInfo ci, @Local StringReader reader) {
        CommandManager commands = MoonlightClient.getCommands();
        if (commands == null || !(screen instanceof ChatScreen)) return;
        String prefix = commands.getPrefix();
        if (!reader.getString().startsWith(prefix)) return;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;

        reader.setCursor(prefix.length());
        if (currentParse == null) {
            currentParse = commands.getDispatcher().parse(reader, connection.getSuggestionsProvider());
        }
        currentParseIsCommand = false;
        currentParseIsMessage = false;

        int cursor = input.getCursorPosition();
        int parseStart = onlyShowIfCursorPastError ? reader.getCursor() : prefix.length();
        if (cursor >= parseStart && (suggestions == null || !keepSuggestions)) {
            ParseResults<ClientSuggestionProvider> parse = currentParse;
            CompletableFuture<Suggestions> pending = commands.getDispatcher().getCompletionSuggestions(parse, cursor);
            pendingSuggestions = pending;
            pending.thenAccept(result -> {
                if (pendingSuggestions == pending) {
                    updateUsageInfo(parse, result);
                }
            });
        }
        ci.cancel();
    }
}
