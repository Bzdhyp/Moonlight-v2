package wtf.moonlight.module.impl.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import wtf.moonlight.MoonlightClient;
import wtf.moonlight.event.Priority;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.player.MoveInputEvent;
import wtf.moonlight.event.impl.player.PlayerTickEvent;
import wtf.moonlight.module.Category;
import wtf.moonlight.module.Module;
import wtf.moonlight.rotation.Rotation;
import wtf.moonlight.utils.MoveUtils;
import wtf.moonlight.utils.RaycastUtils;
import wtf.moonlight.utils.RotationUtils;
import wtf.moonlight.value.impl.BoolValue;
import wtf.moonlight.value.impl.ModeValue;
import wtf.moonlight.value.impl.NumberValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scaffold extends Module {
    private static final List<Block> BLACKLIST = List.of(
            Blocks.AIR,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE,
            Blocks.IRON_BARS,
            Blocks.SNOW,
            Blocks.COAL_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.TORCH,
            Blocks.ANVIL,
            Blocks.NOTE_BLOCK,
            Blocks.JUKEBOX,
            Blocks.TNT,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.LAPIS_ORE,
            Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON,
            Blocks.LEVER,
            Blocks.TALL_GRASS,
            Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL,
            Blocks.CORNFLOWER,
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.VINE,
            Blocks.SUNFLOWER,
            Blocks.LADDER,
            Blocks.FURNACE,
            Blocks.SAND,
            Blocks.CACTUS,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.CRAFTING_TABLE,
            Blocks.COBWEB,
            Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL,
            Blocks.OAK_FENCE,
            Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT
    );
    private static final List<Float> YAWS = List.of(-135.0F, -90.0F, -45.0F, 0.0F, 45.0F, 90.0F, 135.0F, 180.0F);
    private static final float[] PITCHES = {75.0F, 82.0F, 87.0F};
    private static final double RANGE_SQR = 4.5 * 4.5;
    private static final int SEARCH_DEPTH = 6;
    private static final int STRICT = 1;

    private final BoolValue degrees45 = add(new BoolValue("45 Degrees", false));
    private final BoolValue swing = add(new BoolValue("Swing", true));
    private final ModeValue raycast = add(new ModeValue("Raycast", 0, "Normal", "Strict"));
    private final NumberValue rotationSpeed = add(new NumberValue("Rotation Speed", 1, 180, 1, 127, "%.0f"));

    private @Nullable BlockPos blockPos;
    private @Nullable Direction facing;
    private @Nullable Rotation rotation;
    private int oldSlot = -1;
    private float inputYaw;
    private double lengthSqr;

    public Scaffold() {
        super("Scaffold", Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) oldSlot = player.getInventory().getSelectedSlot();
        blockPos = null;
        facing = null;
        rotation = null;
    }

    @Override
    protected void onDisable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && oldSlot != -1) player.getInventory().setSelectedSlot(oldSlot);
    }

    @Subscribe
    private void onTick(PlayerTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;
        Inventory inventory = player.getInventory();
        int slot = findBlockSlot(inventory);
        if (slot != -1 && inventory.getSelectedSlot() != slot) inventory.setSelectedSlot(slot);
        findBlock(player, level);
        Rotation target = getRotation(player, level);
        rotation = target;
        MoonlightClient.getRotations().setRotations(target, rotationSpeed.get());
        place(minecraft.gameMode, player, level);
    }

    @Subscribe(priority = Priority.HIGHEST)
    private void onMoveInput(MoveInputEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Input input = event.getInput();
        int forward = MoveUtils.impulse(input.forward(), input.backward());
        int strafe = MoveUtils.impulse(input.left(), input.right());
        float direction = player.getYRot() + (float) Math.toDegrees(Math.atan2(-strafe, forward));
        inputYaw = Mth.wrapDegrees(Math.round(direction / 45.0F) * 45.0F);
    }

    private void place(@Nullable MultiPlayerGameMode gameMode, LocalPlayer player, ClientLevel level) {
        BlockPos pos = blockPos;
        Direction direction = facing;
        if (gameMode == null || !onAir(player, level) || pos == null || direction == null) return;
        ItemStack stack = player.getInventory().getSelectedItem();
        if (!isValidStack(stack)) return;
        if (!overBlock(MoonlightClient.getRotations().getRotation(), pos, direction)) return;
        BlockHitResult hit = new BlockHitResult(hitVec(pos, direction), direction, pos, false);
        InteractionResult result = gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        if (swing.get() && result instanceof InteractionResult.Success success && success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
            player.swing(InteractionHand.MAIN_HAND, stack.getInteractAnimation(), false);
        }
    }

    private Rotation getRotation(LocalPlayer player, ClientLevel level) {
        Rotation current = rotation;
        if (current == null) return new Rotation(Mth.wrapDegrees(player.getYRot() - 135.0F), 82.0F);
        BlockPos pos = blockPos;
        Direction direction = facing;
        if (!onAir(player, level) || pos == null || direction == null) return current;
        Rotation calculated = RotationUtils.calculate(pos, direction);
        float baseYaw = Mth.wrapDegrees(inputYaw - 180.0F);
        List<Float> yaws = new ArrayList<>(YAWS);
        yaws.sort(Comparator.comparingDouble(yaw -> Math.abs(Mth.wrapDegrees(baseYaw - yaw))));
        yaws.add(calculated.yaw());
        for (float yaw : yaws) {
            if (degrees45.get() && yaw % 90 == 0) continue;
            for (float pitch : PITCHES) {
                Rotation candidate = new Rotation(yaw + random(-0.3, 0.3), pitch + random(-0.3, 0.3));
                if (overBlock(candidate, pos, direction)) return candidate;
            }
            for (int pitch = -90; pitch < 90; pitch++) {
                Rotation candidate = new Rotation(yaw, pitch);
                if (overBlock(candidate, pos, direction)) return candidate;
            }
        }
        return calculated;
    }

    private boolean overBlock(Rotation candidate, BlockPos pos, Direction direction) {
        return raycast.get() == STRICT ? RaycastUtils.overBlock(candidate, pos, direction) : RaycastUtils.overBlock(candidate, pos);
    }

    private void findBlock(LocalPlayer player, ClientLevel level) {
        lengthSqr = RANGE_SQR;
        blockPos = null;
        facing = null;
        if (!onAir(player, level)) return;
        Vec3 eye = player.getEyePosition();
        int baseY = yLevel(player);
        BlockPos base = BlockPos.containing(eye.x, baseY, eye.z);
        if (checkBlock(level, eye, base)) return;
        for (int d = 1; d <= SEARCH_DEPTH; d++) {
            if (checkBlock(level, eye, new BlockPos(base.getX(), baseY - d, base.getZ()))) return;
            for (int x = 0; x <= d; x++) {
                for (int z = 0; z <= d - x; z++) {
                    int down = d - x - z;
                    for (int flipX = 0; flipX <= 1; flipX++) {
                        for (int flipZ = 0; flipZ <= 1; flipZ++) {
                            BlockPos pos = new BlockPos(base.getX() + (flipX == 0 ? x : -x), baseY - down, base.getZ() + (flipZ == 0 ? z : -z));
                            checkBlock(level, eye, pos);
                        }
                    }
                }
            }
        }
    }

    private boolean checkBlock(ClientLevel level, Vec3 eye, BlockPos pos) {
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (Direction direction : Direction.values()) {
            Vec3 normal = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            BlockPos neighbor = pos.relative(direction);
            BlockState state = level.getBlockState(neighbor);
            if (state.getCollisionShape(level, neighbor).isEmpty() || state.getMenuProvider(level, neighbor) != null) continue;
            Vec3 relevant = center.add(normal.scale(0.5)).subtract(eye);
            if (relevant.lengthSqr() > lengthSqr || relevant.dot(normal) < 0.0) continue;
            Direction face = direction.getOpposite();
            if (face == Direction.UP && MoveUtils.isMoving() && !Minecraft.getInstance().options.keyJump.isDown()) continue;
            lengthSqr = relevant.lengthSqr();
            blockPos = neighbor;
            facing = face;
            return true;
        }
        return false;
    }

    private boolean onAir(LocalPlayer player, ClientLevel level) {
        Vec3 eye = player.getEyePosition();
        return level.getBlockState(BlockPos.containing(eye.x, yLevel(player), eye.z)).canBeReplaced();
    }

    private static int yLevel(LocalPlayer player) {
        return Mth.floor(player.getY()) - 1;
    }

    private static int findBlockSlot(Inventory inventory) {
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (isValidStack(inventory.getItem(i))) return i;
        }
        return -1;
    }

    private static boolean isValidStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || item instanceof StandingAndWallBlockItem) return false;
        String name = stack.getDisplayName().getString();
        if (name.contains("Click") || name.contains("点击")) return false;
        Block block = item.getBlock();
        return !(block instanceof VegetationBlock) && !(block instanceof SlabBlock) && !BLACKLIST.contains(block);
    }

    private static Vec3 hitVec(BlockPos pos, Direction face) {
        double offsetX = 0;
        double offsetY = 0;
        double offsetZ = 0;
        if (face != Direction.UP && face != Direction.DOWN) {
            offsetY = 0.08;
        } else {
            offsetX = random(-0.3, 0.3);
            offsetZ = random(-0.3, 0.3);
        }
        if (face == Direction.WEST || face == Direction.EAST) offsetZ += random(-0.3, 0.3);
        if (face == Direction.SOUTH || face == Direction.NORTH) offsetX += random(-0.3, 0.3);
        return Vec3.atCenterOf(pos).add(offsetX, offsetY, offsetZ);
    }

    private static float random(double min, double max) {
        return (float) (min + Math.random() * (max - min));
    }
}
