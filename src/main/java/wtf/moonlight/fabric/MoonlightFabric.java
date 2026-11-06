package wtf.moonlight.fabric;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import wtf.moonlight.Moonlight;

public class MoonlightFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Moonlight.init();
    }
}
//?}
