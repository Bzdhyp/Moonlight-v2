package wtf.moonlight.platform;

import java.nio.file.Path;

public sealed interface Platform {
    Platform INSTANCE =
        /*? if fabric {*/new FabricPlatform();
        /*?} elif neoforge *///new NeoForgePlatform();

    String name();

    Path configDir();

    //? if fabric {
    final class FabricPlatform implements Platform {
        @Override
        public String name() {
            return "Fabric";
        }

        @Override
        public Path configDir() {
            return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        }
    }
    //?} elif neoforge {
    /*final class NeoForgePlatform implements Platform {
        @Override
        public String name() {
            return "NeoForge";
        }

        @Override
        public Path configDir() {
            return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        }
    }
    *///?}
}
