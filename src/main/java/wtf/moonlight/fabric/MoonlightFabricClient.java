package wtf.moonlight.fabric;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import wtf.moonlight.MoonlightClient;

public class MoonlightFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MoonlightClient.init();
    }
}
//?}
