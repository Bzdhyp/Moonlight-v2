package wtf.moonlight.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class Skin implements Supplier<Identifier> {
    private @Nullable Supplier<PlayerSkin> lookup;

    @Override
    public Identifier get() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) return mc.player.getSkin().body().texturePath();
        Supplier<PlayerSkin> current = lookup;
        if (current == null) {
            current = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
            lookup = current;
        }
        return current.get().body().texturePath();
    }
}
