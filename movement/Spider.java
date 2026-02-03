package nuclear.module.impl.movement;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.math.RayTraceUtil;
import nuclear.utils.world.InventoryUtils;

import java.util.Timer;
import java.util.TimerTask;

@Annotation(name = "Spider", type = TypeList.Movement, desc = "Человек паук типо")
public class Spider extends Module {
    public ModeSetting mode = new ModeSetting("Режим", "Байпас", "Байпас", "Шар", "Пакетная Вода");
    public SliderSetting speed = new SliderSetting("Скорость", 0.45f, 0.1f, 0.5f, 0.025f).setVisible(() -> mode.is("Байпас"));
    public final BooleanSetting shift = new BooleanSetting("Эксплоит шифта", true).setVisible(() -> mode.is("Байпас") || mode.is("Пакетная Вода"));
    public SliderSetting delay = new SliderSetting("Задержка", 0.31f, 0.1f, 1.0f, 0.001f).setVisible(() -> mode.is("Пакетная Вода"));
    private boolean wasSneaking;
    private long lastPlacementTime;
    private boolean waterPlaced;

    private java.util.Timer timer = new java.util.Timer();
    private boolean canUse = true;
    private long lastWallJumpMs = 0L;
    private static final long WALL_JUMP_COOLDOWN_MS = 250L;
    private int originalSlot = -1;

    public Spider() {
        addSettings(mode, delay, speed, shift);
    }


    @Override
    public void onEnable() {
        if (mode.is("Пакетная Вода")) {
            originalSlot = mc.player.inventory.currentItem;
        }
        super.onEnable();
    }

    @Override
    public boolean onEvent(Event event) {
        if (!(event instanceof EventMotion motion)) return false;

        switch (mode.get()) {
            case "Байпас" -> handleBypassMode(motion);
            case "Шар" -> handleSphereMode(motion);
            case "Пакетная Вода" -> handleWaterBucketMode(motion);
        }
        return false;
    }

    private void handleBypassMode(EventMotion motion) {
        if (!mc.player.collidedHorizontally) {
            if (shift.get() && wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
                mc.gameSettings.keyBindSneak.setPressed(false);
                wasSneaking = false;
            }
            return;
        }

        mc.player.setMotion(mc.player.getMotion().x, speed.getValue().floatValue(), mc.player.getMotion().z);
        if (shift.get() && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.gameSettings.keyBindSneak.setPressed(true);
            wasSneaking = true;
        }
    }

    private void handleSphereMode(EventMotion motion) {
        if (!mc.player.collidedHorizontally) return;

        if (mc.player.isOnGround()) {
            motion.setOnGround(true);
            mc.player.jump();
        }

        if (mc.player.fallDistance <= 0 || mc.player.fallDistance >= 2) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlacementTime < 100) return;

        ItemStack offHandItem = mc.player.getHeldItemOffhand();
        if (offHandItem.getItem() != Items.PLAYER_HEAD) return;

        RayTraceResult entityTrace = RayTraceUtil.rayTrace(3, motion.getYaw(), 85f, mc.player);
        if (entityTrace.getType() == RayTraceResult.Type.ENTITY) {
            Entity hitEntity = ((net.minecraft.util.math.EntityRayTraceResult) entityTrace).getEntity();
            if (hitEntity != null && !hitEntity.equals(mc.player)) return;
        }

        BlockRayTraceResult blockTrace = (BlockRayTraceResult) RayTraceUtil.rayTrace(3, motion.getYaw(), 85f, mc.player);
        if (blockTrace.getType() == RayTraceResult.Type.BLOCK) {
            BlockPos hitPos = blockTrace.getPos();
            if (mc.world.getBlockState(hitPos).isSolid()) {
                motion.setPitch(85f);
                mc.player.rotationPitchHead = 85f;
                motion.setYaw(mc.player.getHorizontalFacing().getHorizontalAngle());
                mc.player.swingArm(Hand.OFF_HAND);
                mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.OFF_HAND, blockTrace);

                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.gameSettings.keyBindSneak.setPressed(false);
                    wasSneaking = false;
                }
                mc.player.fallDistance = 0;
                lastPlacementTime = currentTime;
            }
        }
    }

    private void handleWaterBucketMode(EventMotion motion) {
        if (mc.player.isInWater() || mc.player.isInLava()) {
            mc.player.motion.y = 0.30f;
            return;
        }

        int waterSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                waterSlot = i;
                break;
            }
        }

        boolean hasWaterBucket = waterSlot != -1 ||
                (!mc.player.getHeldItemMainhand().isEmpty() && mc.player.getHeldItemMainhand().getItem() == Items.WATER_BUCKET);

        hasWaterBucket = hasWaterBucket || InventoryUtils.getItemSlot(Items.WATER_BUCKET) != -1;


        if (!hasWaterBucket || !mc.player.collidedHorizontally) {
            if (shift.get() && wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
                mc.gameSettings.keyBindSneak.setPressed(false);
                wasSneaking = false;
            }

            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                mc.gameSettings.keyBindJump.setPressed(false);
            }
            return;
        }

        if (shift.get() && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.gameSettings.keyBindSneak.setPressed(true);
            wasSneaking = true;
        }

        if (mc.player.isOnGround()) {
            long now = System.currentTimeMillis();
            if (now - lastWallJumpMs > WALL_JUMP_COOLDOWN_MS) {
                mc.player.jump();
                lastWallJumpMs = now;
            }
            mc.gameSettings.keyBindJump.setPressed(true);
        }

        if (canUse) {
            motion.setPitch(70f);
            mc.player.rotationPitchHead = 70f;

            if (waterSlot == -1) {
                InventoryUtils.inventorySwapClick(Items.WATER_BUCKET, false);
            } else {
                int clientSlot = mc.player.inventory.currentItem;
                if (waterSlot != clientSlot) {
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(waterSlot));
                }
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                if (waterSlot != clientSlot) {
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(clientSlot));
                }
            }

            mc.player.motion.y = 0.43;
            canUse = false;

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    canUse = true;
                }
            }, getDelayMs());
        }
    }

    private int getDelayMs() {
        return (int) (delay.getValue().intValue() * 1000f);
    }

    private int findWaterBucketInInventory() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        if (shift.get() && wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.gameSettings.keyBindSneak.setPressed(false);
        }
        wasSneaking = false;
        lastPlacementTime = 0;
        mc.player.fallDistance = 0;
        waterPlaced = false;

        timer.cancel();
        timer = new Timer();
        canUse = true;

        if (mc.gameSettings != null) {
            mc.gameSettings.keyBindSneak.setPressed(false);
            mc.gameSettings.keyBindJump.setPressed(false);
        }

        if (mode.is("Пакетная Вода") && originalSlot != -1 && mc.player.inventory.currentItem != originalSlot) {
            mc.player.inventory.currentItem = originalSlot;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(originalSlot));
        }
        originalSlot = -1;

        super.onDisable();
        return;
    }
}