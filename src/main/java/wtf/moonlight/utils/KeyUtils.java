package wtf.moonlight.utils;

import com.mojang.blaze3d.platform.InputConstants;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KeyUtils {
    public static final int NONE = 0;

    private static final String KEYBOARD_PREFIX = "key.keyboard.";
    private static final int SCANCODE_COUNT = 512;
    private static final int LAST_NAMED_SCANCODE = 290;
    private static final boolean WINDOWS = System.getProperty("os.name", "").startsWith("Windows");
    private static final List<String> MOUSE_NAMES = List.of("LMB", "MMB", "RMB", "MB4", "MB5");
    private static final Map<String, Integer> ALIASES = Map.ofEntries(
            Map.entry("none", NONE),
            Map.entry("unbind", NONE),
            Map.entry("null", NONE),
            Map.entry("lmb", mouseCode(InputConstants.MOUSE_BUTTON_LEFT)),
            Map.entry("mouseleft", mouseCode(InputConstants.MOUSE_BUTTON_LEFT)),
            Map.entry("mmb", mouseCode(InputConstants.MOUSE_BUTTON_MIDDLE)),
            Map.entry("mousemiddle", mouseCode(InputConstants.MOUSE_BUTTON_MIDDLE)),
            Map.entry("rmb", mouseCode(InputConstants.MOUSE_BUTTON_RIGHT)),
            Map.entry("mouseright", mouseCode(InputConstants.MOUSE_BUTTON_RIGHT)),
            Map.entry("lshift", InputConstants.KEY_LSHIFT),
            Map.entry("rshift", InputConstants.KEY_RSHIFT),
            Map.entry("lctrl", InputConstants.KEY_LCONTROL),
            Map.entry("lcontrol", InputConstants.KEY_LCONTROL),
            Map.entry("rctrl", InputConstants.KEY_RCONTROL),
            Map.entry("rcontrol", InputConstants.KEY_RCONTROL),
            Map.entry("lalt", InputConstants.KEY_LALT),
            Map.entry("ralt", InputConstants.KEY_RALT),
            Map.entry("esc", InputConstants.KEY_ESCAPE),
            Map.entry("del", InputConstants.KEY_DELETE),
            Map.entry("ins", InputConstants.KEY_INSERT),
            Map.entry("bs", InputConstants.KEY_BACKSPACE),
            Map.entry("return", InputConstants.KEY_RETURN),
            Map.entry("pgup", InputConstants.KEY_PAGEUP),
            Map.entry("pgdn", InputConstants.KEY_PAGEDOWN),
            Map.entry("caps", InputConstants.KEY_CAPSLOCK)
    );

    private static volatile @Nullable Map<String, Integer> keyboardNames;

    private KeyUtils() {
    }

    public static boolean isMouse(int code) {
        return code < 0;
    }

    public static int mouseCode(int button) {
        return -button;
    }

    public static int mouseButton(int code) {
        return -code;
    }

    public static String getName(int code) {
        if (code == NONE) return "None";
        if (isMouse(code)) {
            int button = mouseButton(code);
            return button <= MOUSE_NAMES.size() ? MOUSE_NAMES.get(button - 1) : "MB" + button;
        }
        String token = keyboardToken(code);
        return token == null ? "Key" + code : compact(token);
    }

    public static @Nullable Integer parse(String input) {
        String wanted = normalize(input);
        if (wanted.isEmpty()) return null;
        Integer alias = ALIASES.get(wanted);
        if (alias != null) return alias;
        if (wanted.startsWith("mouse") || wanted.startsWith("mb")) {
            Integer button = parseNumber(wanted.substring(wanted.startsWith("mouse") ? 5 : 2));
            if (button != null && button > 0) return mouseCode(button);
        }
        if (wanted.startsWith("key")) {
            Integer scancode = parseNumber(wanted.substring(3));
            if (scancode != null && scancode > 0 && scancode < SCANCODE_COUNT) return scancode;
        }
        return keyboardNames().get(wanted);
    }

    public static List<String> suggestions() {
        List<String> names = new ArrayList<>();
        names.add("None");
        names.addAll(MOUSE_NAMES);
        for (int code : keyboardNames().values()) {
            names.add(getName(code));
        }
        return names;
    }

    public static boolean isDown(int code) {
        if (code == NONE) return false;
        if (isMouse(code)) {
            int button = mouseButton(code);
            return button <= 32 && (SDLMouse.nSDL_GetMouseState(0L, 0L) & (1 << (button - 1))) != 0;
        }
        return code < SCANCODE_COUNT && InputConstants.isKeyDown(code);
    }

    public static int scancode(int scancode, int keycode, int raw) {
        if (scancode != NONE) return scancode;
        if (keycode != NONE) {
            int fromKey = SDLKeyboard.nSDL_GetScancodeFromKey(keycode, 0L);
            if (fromKey != NONE) return fromKey;
        }
        if (!WINDOWS) return NONE;
        boolean extended = (raw & 0xFF00) == 0xE000;
        return switch (raw & 0xFF) {
            case 0x2A -> InputConstants.KEY_LSHIFT;
            case 0x36 -> InputConstants.KEY_RSHIFT;
            case 0x1D -> extended ? InputConstants.KEY_RCONTROL : InputConstants.KEY_LCONTROL;
            case 0x38 -> extended ? InputConstants.KEY_RALT : InputConstants.KEY_LALT;
            default -> NONE;
        };
    }

    private static Map<String, Integer> keyboardNames() {
        Map<String, Integer> names = keyboardNames;
        if (names != null) return names;
        Map<String, Integer> built = new LinkedHashMap<>();
        for (int code = 1; code <= LAST_NAMED_SCANCODE; code++) {
            String token = keyboardToken(code);
            if (token != null) built.putIfAbsent(normalize(compact(token)), code);
        }
        names = Collections.unmodifiableMap(built);
        keyboardNames = names;
        return names;
    }

    private static @Nullable String keyboardToken(int code) {
        String name = InputConstants.Type.KEYBOARD.getOrCreate(code).getName();
        String token = name.startsWith(KEYBOARD_PREFIX) ? name.substring(KEYBOARD_PREFIX.length()) : name;
        return token.equals(String.valueOf(code)) ? null : token;
    }

    private static String compact(String token) {
        StringBuilder builder = new StringBuilder(token.length());
        for (String part : token.split("\\.")) {
            if (part.isEmpty()) continue;
            builder.append(Character.toUpperCase(part.charAt(0))).append(part, 1, part.length());
        }
        return builder.toString();
    }

    private static String normalize(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) builder.append(Character.toLowerCase(c));
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private static @Nullable Integer parseNumber(String text) {
        if (text.isEmpty() || !text.chars().allMatch(Character::isDigit)) return null;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
