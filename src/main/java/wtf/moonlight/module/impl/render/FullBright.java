package wtf.moonlight.module.impl.render;

import net.minecraft.client.Minecraft;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.player.PlayerTickEvent;
import wtf.moonlight.event.impl.render.GammaEvent;
import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;

public final class FullBright extends Module {
    private static final double BRIGHTNESS = 15.0;
    private static final double STEP = 0.1;

    private double gamma;

    public FullBright() {
        super("FullBright", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        gamma = Minecraft.getInstance().options.gamma().get();
    }

    @Subscribe
    private void onTick(PlayerTickEvent event) {
        gamma = Math.min(BRIGHTNESS, gamma + STEP);
    }

    @Subscribe
    private void onGamma(GammaEvent event) {
        event.setGamma(gamma);
    }
}
