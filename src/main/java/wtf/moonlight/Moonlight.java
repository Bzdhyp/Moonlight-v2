package wtf.moonlight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.moonlight.event.EventBus;
import wtf.moonlight.platform.Platform;

public final class Moonlight {
    public static final String MOD_ID = "moonlight";
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final EventBus EVENT_BUS = new EventBus();

    private Moonlight() {
    }

    public static void init() {
        LOGGER.info("Moonlight {} loading on {}", VERSION, Platform.INSTANCE.name());
    }
}
