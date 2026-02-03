package nuclear.module.impl.combat;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventInput;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.math.SensUtil;
import nuclear.utils.move.MoveUtil;

import java.util.*;

import static net.minecraft.util.math.MathHelper.clamp;

@Annotation(name = "ProjectileHelper", type = TypeList.Combat, desc = "Прицеливание для метательного оружия с траекторным расчетом")
public class ProjectileHelper extends Module {
    private final MultiBoxSetting weapons = new MultiBoxSetting("Оружие",
            new BooleanSetting("Лук", true),
            new BooleanSetting("Трезубец", false),
            new BooleanSetting("Арбалет", false),
            new BooleanSetting("Снежок", false));
    private final SliderSetting fov = new SliderSetting("Поле зрения", 180.0f, 10.0f, 180.0f, 5f);
    private final BooleanSetting predictPosition = new BooleanSetting("Предсказание позиции", true);
    private final BooleanSetting move = new BooleanSetting("Двигатся на цель", false);

    public LivingEntity target;
    public Vector2f rotate = new Vector2f(0, 0);
    public Vector2f server;
    private boolean aiming;
    private boolean initialAim;
    private boolean wasCharging;
    private int chargeTicks = 0;
    private final Map<LivingEntity, List<Vector3d>> positionHistory = new WeakHashMap<>();
    private final Map<LivingEntity, List<Long>> positionHistoryTimestamps = new WeakHashMap<>();

    public ProjectileHelper() {
        this.addSettings(weapons, fov, predictPosition, move);
        this.rotate = new Vector2f(0, 0);
        this.server = new Vector2f(0, 0);
    }

    public boolean check() {
        return state && aiming && server != null;
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc.player != null && mc.player.isElytraFlying()) {
            reset();
            return false;
        }
        if (event instanceof EventInput e) {
            handleInput(e);
        }
        if (mc.player == null || mc.world == null) {
            reset();
            return false;
        }
        if (event instanceof EventUpdate) {
            boolean charging = validItem();

            if (!charging && wasCharging) {
                reset();
            }
            wasCharging = charging;

            if (!charging) return false;

            updateTarget();

            if (target != null && target.isAlive()) {
                if (!aiming) initialAim = true;
                aim();
            } else {
                aiming = false;
            }
            if (target != null && target.isAlive()) {
                synchronized (positionHistory) {
                    List<Vector3d> positions = positionHistory.computeIfAbsent(target, k -> new ArrayList<>(5));
                    List<Long> timestamps = positionHistoryTimestamps.computeIfAbsent(target, k -> new ArrayList<>(5));
                    positions.add(0, target.getPositionVec());
                    timestamps.add(0, System.currentTimeMillis());

                    if (positions.size() > 5) {
                        positions.remove(positions.size() - 1);
                        timestamps.remove(timestamps.size() - 1);
                    }
                }
            }
        }

        if (event instanceof EventMotion motionEvent) {
            if (target == null || !target.isAlive() || !validItem()) {
                return false;
            }

            motionEvent.setYaw(rotate.x);
            motionEvent.setPitch(rotate.y);
            mc.player.rotationYawHead = rotate.x;
            mc.player.renderYawOffset = rotate.x;
            mc.player.rotationPitchHead = rotate.y;
        }

        return false;
    }

    private void handleInput(EventInput event) {
        if (!move.get()) {
            if (check() && target != null && target.isAlive() && aiming && server != null && rotate != null && mc.player != null) {
                float yawDiff = Math.abs(MathHelper.wrapDegrees(server.x - mc.player.rotationYaw));
                if (yawDiff > 0.1f) {
                    MoveUtil.fixMovement(event, server.x);
                }
            }
        }
    }

    private boolean canAttackThroughWalls(LivingEntity targetEntity) {
        Vector3d targetVec = targetEntity.getPositionVec().add(0.0, (double) targetEntity.getEyeHeight(), 0.0);
        Vector3d playerVec = mc.player.getPositionVec().add(0.0, (double) mc.player.getEyeHeight(), 0.0);
        RayTraceResult result = mc.world.rayTraceBlocks(new RayTraceContext(playerVec, targetVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player));
        return result.getType() == RayTraceResult.Type.MISS;
    }

    private void aim() {
        if (target == null || !target.isAlive()) return;

        float gravity = getGravity();
        Vector3d targetPos;


        if (predictPosition.get()) {
            float modif = 10.0f;
            targetPos = new Vector3d(
                    target.getPosX() + (target.getPosX() - target.lastTickPosX) * modif,
                    target.getPosY() + target.getEyeHeight() - 0.2f,
                    target.getPosZ() + (target.getPosZ() - target.lastTickPosZ) * modif
            );
        } else {
            targetPos = target.getPositionVec().add(0, target.getEyeHeight() - 0.2f, 0);
        }

        if (targetPos == null) return;

        Vector3d playerEyePos = mc.player.getEyePosition(1.0f);
        Vector3d vec = targetPos.subtract(playerEyePos);

        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float horizontalDistance = (float) Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, horizontalDistance)));

        float gravityCorrection = (float) (mc.player.getDistance(target) * gravity);
        pitchToTarget -= gravityCorrection;
        pitchToTarget = clamp(pitchToTarget, -89.0f, 89.0f);

        float currentYaw = mc.player.rotationYaw;
        float yawDelta = MathHelper.wrapDegrees(yawToTarget - currentYaw);
        yawToTarget = currentYaw + yawDelta;

        server = new Vector2f(yawToTarget, pitchToTarget);

        float gcd = SensUtil.getGCDValue();
        if (gcd > 0) {
            yawToTarget -= yawToTarget % gcd;
            pitchToTarget -= pitchToTarget % gcd;
        }

        rotate = new Vector2f(yawToTarget, pitchToTarget);
        aiming = true;
    }

    private float getGravity() {
        ItemStack item = mc.player.getHeldItemMainhand();
        if (item.getItem() instanceof BowItem && weapons.get("Лук")) {
            return 0.17f;
        } else if (item.getItem() instanceof CrossbowItem && weapons.get("Арбалет")) {
            return 0.16f;
        } else if (item.getItem() instanceof TridentItem && weapons.get("Трезубец")) {
            return 0.22f;
        } else if (item.getItem() instanceof SnowballItem && weapons.get("Снежок")) {
            return 0.03f;
        }
        return 0.0f;
    }

    private boolean isBow() {
        ItemStack item = mc.player.getHeldItemMainhand();
        return item.getItem() instanceof BowItem && weapons.get("Лук") && mc.player.isHandActive();
    }

    private boolean isTrident() {
        ItemStack item = mc.player.getHeldItemMainhand();
        return item.getItem() instanceof TridentItem && weapons.get("Трезубец") && mc.player.isHandActive();
    }

    private boolean isCrossbow() {
        ItemStack item = mc.player.getHeldItemMainhand();
        return item.getItem() instanceof CrossbowItem && weapons.get("Арбалет") && CrossbowItem.isCharged(item);
    }

    private boolean isSnowball() {
        ItemStack item = mc.player.getHeldItemMainhand();
        return item.getItem() instanceof SnowballItem && weapons.get("Снежок");
    }

    private void updateTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.world.getAllEntities()) {
            if (isValidTarget(entity)) {
                targets.add((LivingEntity) entity);
            }
        }

        targets.sort(Comparator.comparingDouble(this::getFovToEntity));
        target = targets.isEmpty() ? null : targets.get(0);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        PlayerEntity player = (PlayerEntity) entity;

        if (player instanceof ClientPlayerEntity) return false;
        if (player.ticksExisted < 3) return false;
        if (!canAttackThroughWalls(player)) return false;
        if (mc.player.getDistance(player) > 200) return false;
        if (AntiBot.checkBot(player)) return false;
        if (Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(player.getName().getString())) return false;
        if (player.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) return false;
        if (getFovToEntity(player) > (fov.getValue().floatValue() * 2)) return false;
        if (!player.isAlive()) return false;

        return true;
    }

    private double getFovToEntity(LivingEntity entity) {
        Vector3d playerPos = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = entity.getPositionVec().add(0, entity.getHeight() * 0.5, 0);
        Vector3d vec = targetPos.subtract(playerPos);

        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - mc.player.rotationYaw);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - mc.player.rotationPitch);

        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    private boolean validItem() {
        if (mc.player == null) return false;
        ItemStack item = mc.player.getHeldItemMainhand();
        if (item.isEmpty()) return false;
        return isBow() || isTrident() || isCrossbow() || isSnowball();
    }

    private void reset() {
        if (mc.player != null) {
            rotate = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
            server = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        }
        target = null;
        aiming = false;
        initialAim = false;
        chargeTicks = 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }
}