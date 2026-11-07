package wtf.moonlight.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ChatUtils {
    private static final Component TAG = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("Moonlight").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("] ").withStyle(ChatFormatting.GRAY));

    private ChatUtils() {
    }

    public static void info(String template, Object... args) {
        info(format(template, args, ChatFormatting.GRAY));
    }

    public static void info(Component message) {
        send(Component.empty().append(TAG).append(message));
    }

    public static void error(String template, Object... args) {
        error(format(template, args, ChatFormatting.RED));
    }

    public static void error(Component message) {
        send(Component.empty().append(TAG).append(Component.empty().append(message).withStyle(ChatFormatting.RED)));
    }

    public static void send(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.hud.getChat().addClientSystemMessage(message));
    }

    private static MutableComponent format(String template, Object[] args, ChatFormatting base) {
        MutableComponent message = Component.empty().withStyle(base);
        int start = 0;
        int next = 0;
        for (int index = template.indexOf("{}"); index >= 0; index = template.indexOf("{}", start)) {
            if (index > start) message.append(template.substring(start, index));
            message.append(next < args.length ? highlight(args[next++]) : Component.literal("{}"));
            start = index + 2;
        }
        if (start < template.length()) message.append(template.substring(start));
        return message;
    }

    private static Component highlight(Object value) {
        return value instanceof Component component ? component : Component.literal(String.valueOf(value)).withStyle(ChatFormatting.WHITE);
    }
}
