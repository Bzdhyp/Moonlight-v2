package wtf.moonlight;

public final class MoonlightClient {
    private MoonlightClient() {
    }

    public static void init() {
        Moonlight.LOGGER.info("Moonlight client ready");
    }
}
