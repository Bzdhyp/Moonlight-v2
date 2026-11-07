package wtf.moonlight.module.impl.render;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.MoonlightClient;
import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;
import wtf.moonlight.ui.clickgui.ClickGuiScreen;
import wtf.moonlight.ui.clickgui.Panel;
import wtf.moonlight.ui.imgui.Input;

public final class ClickGui extends Module {
    private @Nullable ClickGuiScreen screen;

    public ClickGui() {
        super("ClickGUI", Category.RENDER);
        getBind().setKey(InputConstants.KEY_RSHIFT);
        getBind().setGate(() -> {
            Screen current = Minecraft.getInstance().gui.screen();
            return current == null || current instanceof ClickGuiScreen && !Input.wantsText();
        });
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    protected void onEnable() {
        Panel panel = MoonlightClient.getPanel();
        if (panel == null) return;
        ClickGuiScreen created = new ClickGuiScreen(panel, this::closed);
        screen = created;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.schedule(() -> {
            if (screen == created) minecraft.gui.setScreen(created);
        });
    }

    @Override
    protected void onDisable() {
        ClickGuiScreen open = screen;
        screen = null;
        if (open != null && Minecraft.getInstance().gui.screen() == open) Minecraft.getInstance().gui.setScreen(null);
    }

    private void closed(ClickGuiScreen closed) {
        if (screen != closed) return;
        screen = null;
        setEnabled(false);
    }
}
