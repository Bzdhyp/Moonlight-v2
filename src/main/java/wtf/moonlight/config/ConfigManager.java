package wtf.moonlight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.Blaze3D;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.client.GameLoadedEvent;
import wtf.moonlight.event.impl.client.TickEvent;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.ModuleManager;
import wtf.moonlight.value.Value;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ConfigManager {
    public static final String LATEST = "latest";

    private static final String EXTENSION = ".json";
    private static final int MAX_NAME = 63;
    private static final int AUTO_SAVE_TICKS = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger("moonlight/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dir;
    private final ModuleManager modules;
    private final List<Value<?>> clientValues;
    private List<String> configs = List.of();
    private String current = LATEST;
    private String status = "";
    private int statusSerial;
    private boolean loaded;
    private int ticks;
    private String lastAutoSave = "";

    public ConfigManager(Path dir, ModuleManager modules, List<Value<?>> clientValues) {
        this.dir = dir;
        this.modules = modules;
        this.clientValues = clientValues;
        Runtime.getRuntime().addShutdownHook(new Thread(this::autoSave, "Moonlight config save"));
    }

    public synchronized boolean save(String name) {
        String json = GSON.toJson(snapshot());
        if (!write(name, json)) {
            setStatus("Could not write the config file");
            return false;
        }
        if (name.equals(LATEST)) lastAutoSave = json;
        current = name;
        refresh();
        setStatus("Saved " + name);
        return true;
    }

    public boolean saveCurrent() {
        return save(current);
    }

    public boolean saveAs(String name) {
        String clean = sanitize(name);
        if (clean.isEmpty()) {
            setStatus("That name cannot be used as a file");
            return false;
        }
        return save(clean);
    }

    public boolean load(String name) {
        JsonObject root = read(name);
        if (root == null) {
            setStatus(configs.contains(name) ? name + " is not a readable config" : "There is no config by that name");
            return false;
        }
        try {
            apply(root);
        } catch (RuntimeException exception) {
            LOGGER.warn("Config {} has invalid values", name, exception);
            setStatus(name + " is not a readable config");
            return false;
        }
        current = name;
        setStatus("Loaded " + name);
        return true;
    }

    public boolean rename(String from, String to) {
        String clean = sanitize(to);
        if (!configs.contains(from)) {
            setStatus("There is no config by that name");
            return false;
        }
        if (clean.isEmpty()) {
            setStatus("That name cannot be used as a file");
            return false;
        }
        if (clean.equals(from)) return true;
        if (configs.contains(clean)) {
            setStatus(clean + " already exists");
            return false;
        }
        try {
            Files.move(file(from), file(clean));
        } catch (IOException exception) {
            LOGGER.warn("Could not rename {}", from, exception);
            setStatus("Could not rename the config file");
            return false;
        }
        refresh();
        if (current.equals(from)) current = clean;
        setStatus("Renamed to " + clean);
        return true;
    }

    public boolean delete(String name) {
        if (!configs.contains(name)) {
            setStatus("There is no config by that name");
            return false;
        }
        try {
            Files.deleteIfExists(file(name));
        } catch (IOException exception) {
            LOGGER.warn("Could not delete {}", name, exception);
            setStatus("Could not delete the config file");
            return false;
        }
        refresh();
        if (name.equals(current)) {
            current = configs.isEmpty() ? LATEST : configs.getFirst();
            JsonObject next = read(current);
            if (next != null) apply(next);
        }
        setStatus("Deleted " + name);
        return true;
    }

    public boolean duplicate(String name) {
        if (!configs.contains(name)) {
            setStatus("There is no config by that name");
            return false;
        }
        String copy = null;
        for (int suffix = 1; suffix < 100 && copy == null; suffix++) {
            String candidate = suffix == 1 ? name + " copy" : name + " copy " + suffix;
            if (!configs.contains(candidate)) copy = candidate;
        }
        if (copy == null) {
            setStatus("Too many copies of " + name);
            return false;
        }
        try {
            Files.copy(file(name), file(copy));
        } catch (IOException exception) {
            LOGGER.warn("Could not copy {}", name, exception);
            setStatus("Could not copy the config file");
            return false;
        }
        refresh();
        setStatus("Duplicated " + name + " as " + copy);
        return true;
    }

    public void refresh() {
        if (!Files.isDirectory(dir)) {
            configs = List.of();
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            configs = files.map(file -> file.getFileName().toString())
                    .filter(file -> file.endsWith(EXTENSION))
                    .map(file -> file.substring(0, file.length() - EXTENSION.length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException exception) {
            LOGGER.warn("Could not list {}", dir, exception);
            configs = List.of();
        }
    }

    public void openFolder() {
        try {
            Files.createDirectories(dir);
        } catch (IOException exception) {
            LOGGER.warn("Could not create {}", dir, exception);
        }
        Blaze3D.openPath(dir);
    }

    public List<String> getConfigs() {
        return configs;
    }

    public String getCurrent() {
        return current;
    }

    public String getStatus() {
        return status;
    }

    public int getStatusSerial() {
        return statusSerial;
    }

    public static boolean isValidName(String name) {
        return !name.isEmpty() && sanitize(name).equals(name);
    }

    public static String sanitize(String name) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length() && out.length() < MAX_NAME; i++) {
            char c = name.charAt(i);
            boolean ascii = c < 128 && Character.isLetterOrDigit(c);
            if (ascii || c == ' ' || c == '-' || c == '_') out.append(c);
            else if (c == '.' && !out.toString().endsWith(EXTENSION)) out.append(c);
        }
        while (!out.isEmpty() && (out.charAt(0) == ' ' || out.charAt(0) == '.')) out.deleteCharAt(0);
        while (!out.isEmpty() && (out.charAt(out.length() - 1) == ' ' || out.charAt(out.length() - 1) == '.')) {
            out.deleteCharAt(out.length() - 1);
        }
        if (out.toString().toLowerCase(Locale.ROOT).endsWith(EXTENSION)) out.setLength(out.length() - EXTENSION.length());
        String clean = out.toString();
        return clean.equals(".") || clean.equals("..") ? "" : clean;
    }

    @Subscribe
    private void onGameLoaded(GameLoadedEvent event) {
        refresh();
        if (configs.contains(LATEST)) load(LATEST);
        synchronized (this) {
            lastAutoSave = GSON.toJson(snapshot());
            loaded = true;
        }
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (event.getPhase().isPre() && ++ticks % AUTO_SAVE_TICKS == 0) autoSave();
    }

    private synchronized void autoSave() {
        if (!loaded) return;
        String json = GSON.toJson(snapshot());
        if (json.equals(lastAutoSave)) return;
        if (write(LATEST, json)) {
            lastAutoSave = json;
            if (!configs.contains(LATEST)) refresh();
        }
    }

    private void setStatus(String text) {
        status = text;
        statusSerial++;
    }

    private JsonObject snapshot() {
        JsonObject moduleJson = new JsonObject();
        for (Module module : modules.getModules()) {
            JsonObject json = new JsonObject();
            if (module.isPersistent()) json.addProperty("enabled", module.isEnabled());
            json.add("bind", module.getBind().save());
            if (!module.getValues().isEmpty()) {
                JsonObject valueJson = new JsonObject();
                for (Value<?> value : module.getValues()) {
                    valueJson.add(value.getName(), value.save());
                }
                json.add("values", valueJson);
            }
            moduleJson.add(module.getName(), json);
        }
        JsonObject clientJson = new JsonObject();
        for (Value<?> value : clientValues) {
            clientJson.add(value.getName(), value.save());
        }
        JsonObject root = new JsonObject();
        root.add("modules", moduleJson);
        root.add("client", clientJson);
        return root;
    }

    private void apply(JsonObject root) {
        if (root.get("modules") instanceof JsonObject moduleJson) {
            for (Module module : modules.getModules()) {
                if (!(moduleJson.get(module.getName()) instanceof JsonObject json)) continue;
                if (json.has("bind")) module.getBind().load(json.get("bind"));
                if (json.get("values") instanceof JsonObject valueJson) {
                    for (Value<?> value : module.getValues()) {
                        JsonElement element = valueJson.get(value.getName());
                        if (element != null) value.load(element);
                    }
                }
                if (module.isPersistent() && json.has("enabled")) module.setEnabled(json.get("enabled").getAsBoolean());
            }
        }
        if (root.get("client") instanceof JsonObject clientJson) {
            for (Value<?> value : clientValues) {
                JsonElement json = clientJson.get(value.getName());
                if (json != null) value.load(json);
            }
        }
    }

    private Path file(String name) {
        return dir.resolve(name + EXTENSION);
    }

    private @Nullable JsonObject read(String name) {
        Path file = file(name);
        if (!Files.isRegularFile(file)) return null;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (IOException | JsonParseException exception) {
            LOGGER.warn("Could not read {}", file, exception);
            return null;
        }
    }

    private boolean write(String name, String json) {
        Path file = file(name);
        Path temp = dir.resolve(name + EXTENSION + ".tmp");
        try {
            Files.createDirectories(dir);
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Could not save {}", file, exception);
            return false;
        }
    }
}
