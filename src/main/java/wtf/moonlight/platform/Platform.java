package wtf.moonlight.platform;

import java.nio.file.Path;

public sealed interface Platform {
    Platform INSTANCE =
        /*? if fabric {*/new FabricPlatform();
        /*?} elif neoforge *///new NeoForgePlatform();

    String name();

    boolean isClient();

    boolean isModLoaded(String id);

    Path configDir();

    //? if fabric {
    final class FabricPlatform implements Platform {
        private final net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();

        @Override
        public String name() {
            return "Fabric";
        }

        @Override
        public boolean isClient() {
            return loader.getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
        }

        @Override
        public boolean isModLoaded(String id) {
            return loader.isModLoaded(id);
        }

        @Override
        public Path configDir() {
            return loader.getConfigDir();
        }
    }
    //?} elif neoforge {
    /*final class NeoForgePlatform implements Platform {
        @Override
        public String name() {
            return "NeoForge";
        }

        @Override
        public boolean isClient() {
            return net.neoforged.fml.loading.FMLEnvironment.getDist().isClient();
        }

        @Override
        public boolean isModLoaded(String id) {
            return net.neoforged.fml.ModList.get().isLoaded(id);
        }

        @Override
        public Path configDir() {
            return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        }
    }
    *///?}
}
