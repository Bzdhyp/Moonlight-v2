package wtf.moonlight;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.moonlight.platform.Platform;

public final class Moonlight {
    public static final String MOD_ID = "moonlight";
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Moonlight() {
    }

    public static void init() {
        LOGGER.info("Moonlight {} loading on {}", VERSION, Platform.INSTANCE.name());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
