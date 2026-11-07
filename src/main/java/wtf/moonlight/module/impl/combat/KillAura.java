package wtf.moonlight.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import wtf.moonlight.MoonlightClient;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.client.TickEvent;
import wtf.moonlight.event.impl.player.PlayerTickEvent;
import wtf.moonlight.event.impl.render.RenderOverlayEvent;
import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;
import wtf.moonlight.module.impl.player.Scaffold;
import wtf.moonlight.rotation.Rotation;
import wtf.moonlight.utils.RaycastUtils;
import wtf.moonlight.utils.RotationUtils;
import wtf.moonlight.value.impl.ListValue;
import wtf.moonlight.value.impl.ModeValue;
import wtf.moonlight.value.impl.NumberValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class KillAura extends Module {
    private static final int SWITCH = 1;
    private static final int HEALTH = 0;
    private static final int FOV = 1;
    private static final int RANGE = 2;
    private static final double CPS_DEVIATION = 5;
    private static final double CHECK_RANGE = 3.0;
    private static final String PLAYERS = "Players";
    private static final String MOBS = "Mobs";
    private static final String ANIMALS = "Animals";
    private static final String VILLAGERS = "Villagers";
    private static final String INVISIBLES = "Invisibles";

    private final ModeValue mode = add(new ModeValue("Mode", 0, "Single", "Switch"));
    private final NumberValue switchDelay = add(new NumberValue("Switch Delay", 0, 500, 1, 100, "%.0f ms", () -> mode.get() == SWITCH));
    private final ModeValue priority = add(new ModeValue("Priority", RANGE, "Health", "FoV", "Range", "None"));
    private final NumberValue cps = add(new NumberValue("CPS", 1, 20, 1, 12, "%.0f"));
    private final NumberValue range = add(new NumberValue("Range", 3, 6, 0.01, 4, "%.2f"));
    private final NumberValue fov = add(new NumberValue("FOV", 0, 360, 1, 180, "%.0f"));
    private final NumberValue rotationSpeed = add(new NumberValue("Rotation Speed", 0, 180, 1, 180, "%.0f"));
    private final ListValue kinds = add(new ListValue("Targets", List.of(PLAYERS, MOBS, ANIMALS, VILLAGERS, INVISIBLES), List.of(PLAYERS)));
    private final Scaffold scaffold;

    private List<LivingEntity> targets = List.of();
    private int targetIndex;
    private long lastSwitch;
    private long lastAttack;
    private int attacks;

    public KillAura(Scaffold scaffold) {
        super("KillAura", Category.COMBAT);
        this.scaffold = scaffold;
    }

    @Override
    protected void onDisable() {
        targets = List.of();
        attacks = 0;
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (!event.getPhase().isPre()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || scaffold.isEnabled()) {
            targets = List.of();
            return;
        }
        List<LivingEntity> found = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && isTarget(player, living)
                    && RotationUtils.getDistance(living) <= range.get() && angleTo(player, living) <= fov.get() / 2) {
                found.add(living);
            }
        }
        switch (priority.get()) {
            case HEALTH -> found.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            case FOV -> found.sort(Comparator.comparingDouble(living -> angleTo(player, living)));
            case RANGE -> found.sort(Comparator.comparingDouble(living -> living.distanceTo(player)));
        }
        targets = found;
        if (mode.get() == SWITCH) {
            long now = System.currentTimeMillis();
            if (now - lastSwitch >= switchDelay.get()) {
                lastSwitch = now;
                targetIndex++;
            }
        } else {
            targetIndex = 0;
        }
        if (targetIndex >= found.size()) targetIndex = 0;
        if (found.isEmpty()) return;
        LivingEntity target = found.get(targetIndex);
        Rotation rotation = RotationUtils.calculate(target, true, range.get());
        HitResult hit = RaycastUtils.rayCast(rotation, range.get());
        if (hit == null || hit.getType() == HitResult.Type.BLOCK) return;
        MoonlightClient.getRotations().setRotations(rotation, rotationSpeed.get(),
                candidate -> RaycastUtils.rayCast(candidate, CHECK_RANGE) instanceof EntityHitResult result && result.getEntity() == target);
    }

    @Subscribe
    private void onRender(RenderOverlayEvent event) {
        if (targets.isEmpty()) return;
        long now = System.currentTimeMillis();
        long delay = (long) (1000.0 / Math.max(0.01, ThreadLocalRandom.current().nextGaussian(cps.get(), CPS_DEVIATION)));
        if (now - lastAttack >= delay) {
            attacks++;
            lastAttack = now;
        }
    }

    @Subscribe
    private void onPlayerTick(PlayerTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        if (player == null || gameMode == null) return;
        while (attacks > 0) {
            attacks--;
            if (player.isUsingItem()) return;
            if (minecraft.hitResult instanceof EntityHitResult hit) {
                gameMode.attack(player, hit.getEntity());
                player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
                player.connection.send(ServerboundPunchPacket.INSTANCE);
            }
        }
    }

    private boolean isTarget(LocalPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive()) return false;
        if (entity instanceof Player && entity.isSpectator()) return false;
        if (entity.isInvisible() && !kinds.has(INVISIBLES)) return false;
        if (entity instanceof Player) return kinds.has(PLAYERS);
        if (entity instanceof AbstractVillager) return kinds.has(VILLAGERS);
        if (entity instanceof Monster || entity instanceof Slime || entity instanceof EnderDragon) return kinds.has(MOBS);
        if (entity instanceof Animal || entity instanceof AmbientCreature || entity instanceof WaterAnimal) return kinds.has(ANIMALS);
        return false;
    }

    private static float angleTo(LocalPlayer player, Entity entity) {
        return Math.abs(Mth.wrapDegrees(player.getYRot() - RotationUtils.calculate(entity).yaw()));
    }
}
