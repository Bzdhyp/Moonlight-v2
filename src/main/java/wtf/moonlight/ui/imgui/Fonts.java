package wtf.moonlight.ui.imgui;

import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class Fonts {
    private static final int ATLAS_SIZE = 2048;

    private static ImFont regular;
    private static ImFont icons;
    private static ImFont wordmark;

    private Fonts() {
    }

    public static ImFont regular() {
        return regular;
    }

    public static ImFont icons() {
        return icons;
    }

    public static ImFont wordmark() {
        return wordmark;
    }

    static void load(ImFontAtlas atlas) {
        atlas.setTexMinWidth(ATLAS_SIZE);
        atlas.setTexMinHeight(ATLAS_SIZE);
        ImFontConfig panel = new ImFontConfig();
        panel.setOversampleH(3);
        panel.setPixelSnapH(true);
        ImFontConfig merge = new ImFontConfig();
        merge.setMergeMode(true);
        ImFontConfig plain = new ImFontConfig();
        Path dir = null;
        try {
            dir = Files.createTempDirectory("moonlight-fonts");
            regular = new ImFont(atlas.addFontFromFileTTF(extract(dir, "sst-medium.ttf"), 15, panel).ptr);
            atlas.addFontFromFileTTF(extract(dir, "medium.ttf"), 15, merge);
            icons = new ImFont(atlas.addFontFromFileTTF(extract(dir, "fa-solid-900.ttf"), 13, panel).ptr);
            wordmark = new ImFont(atlas.addFontFromFileTTF(extract(dir, "extrabold.ttf"), 24, plain).ptr);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            panel.destroy();
            merge.destroy();
            plain.destroy();
            if (dir != null) delete(dir);
        }
    }

    private static String extract(Path dir, String name) throws IOException {
        Path file = dir.resolve(name);
        try (InputStream stream = Fonts.class.getResourceAsStream("/assets/moonlight/fonts/" + name)) {
            if (stream == null) throw new IOException("Missing font " + name);
            Files.copy(stream, file);
        }
        return file.toString();
    }

    private static void delete(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        } catch (IOException exception) {
            dir.toFile().deleteOnExit();
        }
    }
}
