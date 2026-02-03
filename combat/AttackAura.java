package nuclear.module.impl.combat;

import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.Manager;
import nuclear.control.cmd.impl.TargetCmd;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.player.*;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.StopWatch;
import nuclear.utils.math.*;
import nuclear.utils.misc.PredictUtility;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.world.InventoryUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.*;
import static net.minecraft.client.Minecraft.player;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
import static net.optifine.CustomColors.random;

@SuppressWarnings("all")
@Annotation(name = "AttackAura", type = TypeList.Combat, desc = "Бьёт женщин и детей")
public class AttackAura extends Module {
    @Getter
    public static LivingEntity target = null;
    public static Vector2f rotate = new Vector2f(0, 0);
    public Vector2f visualRotate = new Vector2f(0, 0);

    private LivingEntity selected = null;
    private long cachedCurrentTime;
    private LivingEntity cachedPriorityTarget;
    private String lastPriorityName;

    private boolean isSpinning = false;
    private boolean spinDirection = true;
    private float spinProgress = 0.0f;

    private final ModeSetting sortMode = new ModeSetting("Сортировать", "Умная", "Умная", "Поле зрения", "Дистанция", "Здоровье");
    public final ModeSetting rotationMode = new ModeSetting("Режим рот", "Smooth", "Smooth", "Snap", "Spooky", "FunTime", "HolyWorld", "Legit");
    private final MultiBoxSetting targets = new MultiBoxSetting("Выбор целей",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Голые", true),
            new BooleanSetting("Мобы", false),
            new BooleanSetting("Друзья", false),
            new BooleanSetting("Невидимки", true),
            new BooleanSetting("Голые невидимки", true)
    );

    public final ModeSetting sprintMode = new ModeSetting("Режим спринта", "Обычный", "Обычный", "Легитный");
    public static SliderSetting distance = new SliderSetting("Дистанция аттаки", 3.0f, 2.5f, 6f, 0.1f);
    private final SliderSetting rotateDistance = new SliderSetting("Доп ротация", 0.0f, 0.0f, 3.0f, 0.1f);
    private final SliderSetting speedLegit = new SliderSetting("Скорость", 0.2f, 0.1f, 2f, 0.1f).setVisible(() -> rotationMode.is("Legit"));
    private final SliderSetting speedelytra = new SliderSetting("Скорость на элитре", 0.4f, 0.1f, 5f, 0.1f).setVisible(() -> rotationMode.is("Legit"));
    private final SliderSetting elytrarotate = new SliderSetting("Ротация на элитре", 12.5F, 0.0F, 64.0F, 0.5F).setVisible(() -> !rotationMode.is("FunTime"));
    private final SliderSetting elytradist = new SliderSetting("Элитра дистанция", 0.7F, 0.0F, 0.7F, 0.05F).setVisible(() -> !rotationMode.is("FunTime"));
    public final ModeSetting correction = new ModeSetting("Корекция", "Свободная", "Свободная", "Приследование", "Сфокусированная");
    boolean isRotated;

    @Getter
    private final StopWatch stopWatch = new StopWatch();

    public final MultiBoxSetting settings = new MultiBoxSetting("Настройки",
            new BooleanSetting("Только критами", true),
            new BooleanSetting("Отжимать щит", true),
            new BooleanSetting("Ломать щит", true),
            new BooleanSetting("Умные криты", false),
            new BooleanSetting("Бить через стены", true),
            new BooleanSetting("Не бить если ешь", true));

    public final BooleanSetting rwbupas = new BooleanSetting("Бить через стены RW", true).setVisible(() -> settings != null && settings.get("Бить через стены"));

    float ticksUntilNextAttack;
    private long cpsLimit = 0;
    private long lastRandomCooldown = 0;
    private long randomCooldownDelay = 0;
    public float speedtop = 0.0f;
    public double speed = (double) 0.0F;
    private boolean slothRaytraceNext = false;
    private long lastAttackTime = 0;
    private boolean attackSoundReceived = false;
    private static final long SOUND_WAIT_TIME = 160;

    private Vector3d currentRandomVector = null;
    private boolean usingRandomVector = false;
    private long randomVectorSwitchTime = 0;
    private Vector3d lastRandomVector = null;
    private double currentVectorDistance = 0;
    private static final double VECTOR_CHECK_RADIUS = 1.2;
    private static final long VECTOR_TIMEOUT_MS = 650;

    private final List<net.minecraft.network.IPacket<?>> frozenPackets = new ArrayList<>();
    private boolean defensiveActive = false;
    private boolean lastDefensiveActive = false;
    private Vector3d defensivePos = null;
    private long freezeStartTime = 0;
    private static final long FREEZE_DURATION_MS = 350;

    public AttackAura() {
        this.addSettings(targets, sortMode, rotationMode, correction, distance, rotateDistance, speedLegit, sprintMode, elytrarotate, elytradist, speedelytra, settings, rwbupas);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventInteractEntity entity) {
            if (target != null) entity.setCancel(true);
        }

        if (event instanceof EventRender e) {
            if (mc.player != null && !mc.player.isElytraFlying() && (rotationMode.is("HolyWorld"))) {
                updateVisualRotation();
            }
            if (e.isRender3D() && defensivePos != null && defensiveActive && Manager.FUNCTION_MANAGER != null &&
                    Manager.FUNCTION_MANAGER.elytraResolver != null &&
                    Manager.FUNCTION_MANAGER.elytraResolver.freezeDummy.get()) {
                renderFrozenPosition();
            }
        }
        if (event instanceof EventPacket e) {
            onPacket(e);
            handleDesync(e);
        }

        if (event instanceof EventInput eventInput) {
            if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.freeCam != null &&
                    Manager.FUNCTION_MANAGER.freeCam.player == null) {
                if (correction.is("Свободная")) {
                    if (Manager.FUNCTION_MANAGER.autoPotionFunction != null && Minecraft.getInstance().player != null) {
                        MoveUtil.fixMovement(eventInput, Manager.FUNCTION_MANAGER.autoPotionFunction.isActivePotion ? Minecraft.getInstance().player.rotationYaw : rotate.x);
                    } else {
                        MoveUtil.fixMovement(eventInput, rotate.x);
                    }
                } else if (correction.is("Приследование")) {
                    MoveUtil.fixMovementToTarget(eventInput, target);
                }
            }
        }

        if (event instanceof EventUpdate e) {
            if (target == null || !isValidTarget(target)) {
                target = findTarget();
            }
        }

        if (target != null && mc.world != null && mc.player != null && distance != null && rotateDistance != null && elytrarotate != null && mc.world.getEntityByID(target.getEntityId()) != null && isValidTarget(target)) {
            double targetDistance = getDistance(target);
            float elytrarotate1 = 0.0F;
            if (mc.player.isElytraFlying()) {
                elytrarotate1 = this.elytrarotate.getValue().floatValue();
            }
            double maxDist = distance.getValue().floatValue() + rotateDistance.getValue().floatValue() + elytrarotate1;

            if (targetDistance <= maxDist) {
                if (rotationMode.is("Legit")) {
                    float[] targetRotations = calculateRotations(target);
                    if (targetRotations != null && targetRotations.length >= 2 && speedelytra != null && speedLegit != null) {
                        mc.player.rotationYaw = smoothRotation(mc.player.rotationYaw, targetRotations[0], (mc.player.isElytraFlying() ? speedelytra.getValue().floatValue() : speedLegit.getValue().floatValue()) / 300.0f);
                        mc.player.rotationPitch = smoothRotation(mc.player.rotationPitch, targetRotations[1], (mc.player.isElytraFlying() ? speedelytra.getValue().floatValue() : speedLegit.getValue().floatValue()) / 850f);
                        rotate = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
                        lastYaw = mc.player.rotationYaw;
                        lastPitch = mc.player.rotationPitch;
                    }
                }
            } else {
                target = null;
            }
        }

        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.packetCriticals != null &&
                Manager.FUNCTION_MANAGER.packetCriticals.state && mc.player != null &&
                (mc.player.isElytraFlying() || mc.player.isPotionActive(Effects.SLOW_FALLING) || mc.player.isPotionActive(Effects.LEVITATION))) {
            if (event instanceof EventPostUpdate e) {
                cachedCurrentTime = System.currentTimeMillis();
                LivingEntity priorityTarget = findPriorityTarget();
                LivingEntity oldTarget = target;

                if (priorityTarget != null) {
                    target = priorityTarget;
                } else if (!(target != null && isValidTarget(target))) {
                    target = findTarget();
                }

                if (oldTarget != target) {
                    currentRandomVector = null;
                    usingRandomVector = false;
                    randomVectorSwitchTime = 0;
                    lastRandomVector = null;
                    currentVectorDistance = 0;
                    freezeStartTime = 0;
                }

                if (target == null) {
                    cpsLimit = System.currentTimeMillis();
                    rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
                    speedtop = 0.0f;
                    slothRaytraceNext = false;
                    currentRandomVector = null;
                    usingRandomVector = false;
                    randomVectorSwitchTime = 0;
                    lastRandomVector = null;
                    currentVectorDistance = 0;
                    freezeStartTime = 0;
                    return false;
                }

                attackAndRotateOnEntity(target);
            }
        } else {
            if (event instanceof EventUpdate updateEvent) {
                cachedCurrentTime = System.currentTimeMillis();
                LivingEntity priorityTarget = findPriorityTarget();
                LivingEntity oldTarget = target;

                if (priorityTarget != null) {
                    target = priorityTarget;
                } else if (!(target != null && isValidTarget(target))) {
                    target = findTarget();
                }

                // Сброс состояния векторов при смене цели
                if (oldTarget != target) {
                    currentRandomVector = null;
                    usingRandomVector = false;
                    randomVectorSwitchTime = 0;
                    lastRandomVector = null;
                    currentVectorDistance = 0;
                    freezeStartTime = 0;
                }

                if (target == null) {
                    cpsLimit = System.currentTimeMillis();
                    rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
                    speedtop = 0.0f;
                    slothRaytraceNext = false;
                    currentRandomVector = null;
                    usingRandomVector = false;
                    randomVectorSwitchTime = 0;
                    lastRandomVector = null;
                    currentVectorDistance = 0;
                    freezeStartTime = 0;
                    return false;
                }

                attackAndRotateOnEntity(target);
            }
        }

        if (event instanceof EventUpdate || event instanceof EventPostUpdate) {
            if (mc.player != null && mc.player.isElytraFlying()) {
                updateDefensiveActive();
            }
            if (!defensiveActive && !frozenPackets.isEmpty() && mc.player != null &&
                    mc.player.connection != null && mc.player.connection.getNetworkManager() != null) {
                for (IPacket<?> p : frozenPackets) {
                    mc.player.connection.getNetworkManager().sendPacketSilent(p);
                }
                frozenPackets.clear();
                if (defensivePos != null) {
                    defensivePos = null;
                }
            }
        }

        if (event instanceof EventMotion motionEvent) {
            handleMotionEvent(motionEvent);
        }

        return false;
    }

    private float oscillatorTime = 0f;
    private final float OSCILLATOR_SPEED = 2.0f;
    private final float YAW_AMPLITUDE = 2;
    private final float PITCH_AMPLITUDE = 2;

    private void updateVisualRotation() {
        if (target == null || mc.player == null || player == null) {
            if (player != null) {
                visualRotate = new Vector2f(player.rotationYaw, player.rotationPitch);
            }
            oscillatorTime = 0f;
            return;
        }

        Vector3d targetPos = target.getPositionVec().add(0.0, target.getHeight() / 2.0, 0.0);
        Vector3d playerPos = mc.player.getEyePosition(1.0F);
        Vector3d diff = targetPos.subtract(playerPos);

        float targetYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));

        float lerpFactor = 0.27f;

        oscillatorTime += 0.05f;
        float circlePhase = oscillatorTime * OSCILLATOR_SPEED;

        float yawOsc = (float) (YAW_AMPLITUDE * Math.sin(circlePhase));
        float pitchOsc = (float) (PITCH_AMPLITUDE * Math.cos(circlePhase));

        targetYaw += yawOsc;
        targetPitch += pitchOsc;

        float yawDiff = MathHelper.wrapDegrees(targetYaw - visualRotate.x);
        float pitchDiff = targetPitch - visualRotate.y;

        float newYaw = visualRotate.x + yawDiff * lerpFactor;
        float newPitch = clamp(visualRotate.y + pitchDiff * lerpFactor, -89.0F, 89.0F);

        visualRotate = new Vector2f(newYaw, newPitch);
    }

    public double getScale(Vector3d position, double size) {
        if (position == null || mc == null || mc.getRenderManager() == null ||
                mc.getRenderManager().info == null || mc.gameRenderer == null) {
            return 1.0;
        }
        Vector3d cam = mc.getRenderManager().info.getProjectedView();
        if (cam == null) return 1.0;
        double distance = cam.distanceTo(position);
        double fov = mc.gameRenderer.getFOVModifier(mc.getRenderManager().info, mc.getRenderPartialTicks(), true);
        return max(10.0, 1000.0 / distance) * (size / 30.0) / (fov == 70.0 ? 1.0 : fov / 70.0);
    }

    public Vector2f clientRot = null;

    private void handleMotionEvent(EventMotion motionEvent) {
        if (target == null || mc.player == null ||
                (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.autoPotionFunction != null &&
                        Manager.FUNCTION_MANAGER.autoPotionFunction.isActivePotion)) return;

        float yaw;
        float pitch;

        yaw = rotate.x;
        pitch = rotate.y;

        motionEvent.setYaw(yaw);
        motionEvent.setPitch(pitch);

        if ((!mc.player.isElytraFlying() && (rotationMode.is("HolyWorld")))) {
            mc.player.rotationYawHead = visualRotate.x;
            mc.player.renderYawOffset = visualRotate.x;
            mc.player.rotationPitchHead = visualRotate.y;
        } else {
            mc.player.rotationYawHead = yaw;
            mc.player.renderYawOffset = yaw;
            mc.player.rotationPitchHead = pitch;
        }
    }

    float lastYaw, lastPitch;

    private void attackAndRotateOnEntity(LivingEntity target) {
        if (target == null || mc.player == null || mc.world == null || player == null) return;

        this.isRotated = true;
        Vector3d vec = AuraUtil.calculateTargetVector(target);
        if (vec == null) return;

        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.elytraMotion != null &&
                Manager.FUNCTION_MANAGER.elytraMotion.state
                && mc.player.isElytraFlying()
                && Manager.FUNCTION_MANAGER.elytraMotion.distancie != null &&
                getDistance(target) <= Manager.FUNCTION_MANAGER.elytraMotion.distancie.getValue().floatValue()) {
            boolean shouldPredictElytra = target.isElytraFlying()
                    && Manager.FUNCTION_MANAGER.elytraPredict != null
                    && Manager.FUNCTION_MANAGER.elytraPredict.state
                    && Manager.FUNCTION_MANAGER.elytraPredict.canPredict(target);
            if (!shouldPredictElytra) {
                mc.player.setVelocity(0, 0, 0);
            }
        }

        if (player != null && player.isElytraFlying() && target.isElytraFlying() &&
                Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.elytraPredict != null &&
                Manager.FUNCTION_MANAGER.elytraPredict.state) {
            rotationAngles(target);
        } else {
            float yawToTarget = (float) wrapDegrees(toDegrees(atan2(vec.z, vec.x)) - 90.0);
            float pitchToTarget = (float) (-toDegrees(atan2(vec.y, hypot(vec.x, vec.z))));

            float yawDelta = wrapDegrees(yawToTarget - this.rotate.x);
            float pitchDelta = wrapDegrees(pitchToTarget - this.rotate.y);

            int roundedYaw = (int) yawDelta;
            boolean elytraFly = false;
            float rotationYawSpeed = 300;
            float rotationPitchSpeed = 300;
            float clampedYaw = min(max(abs((long) yawDelta), 0.0f), rotationYawSpeed);
            float clampedPitch = max(abs((long) pitchDelta), 0.0f);
            float yaw;
            float pitch;
            float gcd = SensUtil.getGCDValue();
            vec = vec.add(
                    (target.getPosX() - target.lastTickPosX),
                    (target.getPosY() - target.lastTickPosY),
                    (target.getPosZ() - target.lastTickPosZ)
            );

            if (player.isElytraFlying()) {
                if (this.shouldAttack(target)) {
                    if (settings != null && settings.options != null && settings.options.size() > 5 && settings.get(5) &&
                            player != null && player.isHandActive() && !player.isBlocking()) return;
                    attackEntityAndSwing(target);
                }


                double vectorSwitchDistance = 1.5;
                if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.elytraResolver != null &&
                        Manager.FUNCTION_MANAGER.elytraResolver.elytradistance != null) {
                    vectorSwitchDistance = Manager.FUNCTION_MANAGER.elytraResolver.elytradistance.getValue().doubleValue();
                }

                double distanceToTarget = getDistanceToTargetVector(target);
                long currentTime = System.currentTimeMillis();

                if (usingRandomVector && currentRandomVector != null) {
                    long timeSinceSwitch = currentTime - randomVectorSwitchTime;
                    boolean isNearVector = isNearRandomVector(currentRandomVector, vectorSwitchDistance);

                    if (isNearVector || timeSinceSwitch >= VECTOR_TIMEOUT_MS) {
                        usingRandomVector = false;
                        currentRandomVector = null;
                        randomVectorSwitchTime = 0;
                        currentVectorDistance = 0;
                        freezeStartTime = 0;
                        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.elytraResolver != null &&
                                Manager.FUNCTION_MANAGER.elytraResolver.autoF != null &&
                                Manager.FUNCTION_MANAGER.elytraResolver.autoF.get()) {
                            useFirework();
                        }
                    } else {
                        double actualDistance = currentVectorDistance > 0 ? currentVectorDistance : vectorSwitchDistance;
                        Vector3d targetPos = target.getPositionVec().add(0.0, target.getHeight() / 2.0, 0.0);
                        Vector3d direction = currentRandomVector.normalize();
                        Vector3d vectorPoint = targetPos.add(direction.scale(actualDistance));

                        Vector3d playerPos = mc.player.getEyePosition(1.0f);
                        Vector3d toVector = vectorPoint.subtract(playerPos);

                        float randomYaw = (float) Math.toDegrees(Math.atan2(toVector.z, toVector.x)) - 90.0f;
                        float randomPitch = (float) (-Math.toDegrees(Math.atan2(toVector.y, Math.hypot(toVector.x, toVector.z))));

                        float randomYawDelta = wrapDegrees(randomYaw - rotate.x);
                        float randomPitchDelta = wrapDegrees(randomPitch - rotate.y);

                        float randomClampedYaw = min(max(abs((long) randomYawDelta), 0.0f), rotationYawSpeed);
                        float randomClampedPitch = max(abs((long) randomPitchDelta), 0.0f);

                        yaw = rotate.x + (randomYawDelta > 0 ? randomClampedYaw : -randomClampedYaw);
                        pitch = clamp(rotate.y + (randomPitchDelta > 0 ? randomClampedPitch : -randomClampedPitch), -89.0F, 89.0F);
                        yaw -= (yaw - rotate.x) % gcd;
                        pitch -= (pitch - rotate.y) % gcd;
                        rotate = new Vector2f(yaw, pitch);
                        this.lastYaw = randomClampedYaw;
                        this.lastPitch = randomClampedPitch;
                        return;
                    }
                }

                if (distanceToTarget < VECTOR_CHECK_RADIUS && !usingRandomVector) {
                    currentRandomVector = getRandomVectorFromElytraResolver();
                    if (currentRandomVector != null) {
                        usingRandomVector = true;
                        randomVectorSwitchTime = currentTime;

                        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.elytraResolver != null &&
                                Manager.FUNCTION_MANAGER.elytraResolver.autoF != null &&
                                Manager.FUNCTION_MANAGER.elytraResolver.autoF.get()) {
                            useFirework();
                        }
                    }
                }
                double eyeOffsetY = clamp(target.getPosY() - target.getHeight(), 0, target.getHeight() / 2.0);
                Vector3d aimPoint = target.getPositionVec().add(0, eyeOffsetY, 0);

                Vector3d playerEyePos = mc.player.getEyePosition(1.0f);

                Vector3d direction = aimPoint.subtract(playerEyePos);

                double rawYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0;

                float currentYaw = rotate.x;

                float deltaYaw = (float) (((rawYaw - currentYaw) + 180) % 360 - 180);

                float targetYaw = currentYaw + deltaYaw;

                double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
                float rawPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontalDistance));

                float targetPitch = clamp(rawPitch, -90.0f, 90.0f);

                Vector2f correctedRotation = correctRotation(targetYaw, targetPitch);

                rotate = new Vector2f(correctedRotation.x, correctedRotation.y);

            } else {
                switch (rotationMode.getIndex()) {
                    case 5 -> {
                        if (vec == null) break;
                        Vector2f currentAngle = new Vector2f(rotate.x, rotate.y);
                        Vector2f targetAngle = calculateTargetAngleForSloth(target, vec);
                        if (targetAngle == null) break;

                        Vector2f angleDelta = calculateAngleDeltaForSloth(currentAngle, targetAngle);
                        if (angleDelta == null) break;
                        yawDelta = angleDelta.x;
                        pitchDelta = angleDelta.y;
                        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
                        boolean canAttack = target != null && shouldAttack(target);

                        float distanceToTarget = 0;
                        if (target != null) {
                            distanceToTarget = (float) mc.player.getDistance(target);
                        }

                        float baseSpeed = canAttack ? 0.87F : 0.56F;

                        float speed = baseSpeed;
                        if (distanceToTarget > 0 && distanceToTarget < 0.66F) {
                            float closeRangeSpeed = clamp(distanceToTarget / 1.5F * 0.35F, 0.1F, 0.6F);
                            speed = canAttack ? 0.85f : Math.min(speed, closeRangeSpeed);
                        }
                        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
                        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
                        float jitterYaw = canAttack ? 0 : (float) (randomLerpForSloth(18, 27) * Math.sin(System.currentTimeMillis() / 50D));
                        float jitterPitch = canAttack ? 0 : (float) (randomLerpForSloth(15, 22) * Math.sin(System.currentTimeMillis() / 13D));

                        if ((!this.isState() || target == null) && stopWatch.finished(1000)) {
                            baseSpeed = 0.35F;
                            jitterYaw = 0;
                            jitterPitch = 0;
                        }
                        float moveYaw = clamp(yawDelta, -lineYaw, lineYaw);
                        float movePitch = clamp(pitchDelta, -linePitch, linePitch);
                        Vector2f moveAngle = new Vector2f(currentAngle.x, currentAngle.y);
                        moveAngle = new Vector2f(
                                MathHelper.lerp(baseSpeed, currentAngle.x, currentAngle.x + moveYaw) + jitterYaw,
                                MathHelper.lerp(baseSpeed, currentAngle.y, currentAngle.y + movePitch) + jitterPitch
                        );

                        if (this.shouldAttack(target)) {
                            if (settings != null && settings.options != null && settings.options.size() > 5 && settings.get(5) &&
                                    player != null && player.isHandActive() && !player.isBlocking()) return;
                            attackEntityAndSwing(target);
                        }

                        rotate = moveAngle;
                    }
                    case 3 -> {
                        if (shouldAttack(target) && isWithinFOV(target, 110.0f)) {
                            if (settings != null && settings.options != null && settings.options.size() > 5 && settings.get(5) &&
                                    player != null && player.isHandActive() && !player.isBlocking()) return;
                            attackEntityAndSwing(target);
                        }
                        if (ticksUntilNextAttack > 0) {
                            setRotation(target, false);
                            --ticksUntilNextAttack;
                        } else {
                            if (player != null) {
                                rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
                            }
                        }
                    }
                    case 2 -> {
                        if (this.shouldAttack(target)) {
                            attackEntityAndSwing(target);
                        }
                        spookyTimeRotation();
                    }
                    case 1 -> {
                        if (shouldAttack(target)) {
                            if (settings != null && settings.options != null && settings.options.size() > 5 && settings.get(5) &&
                                    player != null && player.isHandActive() && !player.isBlocking()) return;
                            attackEntityAndSwing(target);
                        }
                        if (ticksUntilNextAttack > 0) {
                            setRotation(target, false);
                            --ticksUntilNextAttack;
                        } else {
                            if (player != null) {
                                rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
                            }
                        }
                    }
                    case 0 -> {
                        if (shouldAttack(target)) {
                            attackEntityAndSwing(target);
                        }

                        if (vec == null) break;
                        yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
                        pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));

                        yawDelta = MathHelper.wrapDegrees(yawToTarget - this.rotate.x);
                        pitchDelta = MathHelper.wrapDegrees(pitchToTarget - this.rotate.y);

                        clampedYaw = Math.max(Math.abs(yawDelta), 0.0f);
                        clampedPitch = Math.max(Math.abs(pitchDelta), 0.0f);

                        yaw = rotate.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
                        pitch = clamp(rotate.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

                        gcd = SensUtil.getGCDValue();
                        yaw -= (yaw - rotate.x) % gcd;
                        pitch -= (pitch - rotate.y) % gcd;

                        rotate = new Vector2f(yaw, pitch);
                    }
                }
            }
        }
    }


    private float smoothRotation(float current, float target, float speed) {
        float delta = MathHelper.wrapDegrees(target - current);

        float dynamicSpeed = speed * (1 + Math.abs(delta) / 180.0f);
        dynamicSpeed = Math.min(dynamicSpeed, 30.0f);

        float adjustment = delta * dynamicSpeed * 0.05f;
        return current + clamp(adjustment, -dynamicSpeed, dynamicSpeed);
    }

    private Vector2f calculateAngleDeltaForSloth(Vector2f currentAngle, Vector2f targetAngle) {
        float yawDelta = MathHelper.wrapDegrees(targetAngle.x - currentAngle.x);
        float pitchDelta = MathHelper.wrapDegrees(targetAngle.y - currentAngle.y);
        return new Vector2f(yawDelta, pitchDelta);
    }

    private Vector2f calculateTargetAngleForSloth(LivingEntity target, Vector3d vec) {
        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));

        if (target != null && ClientUtils.collide(target, -0.4f)) {
            pitchToTarget = 85.0f;
        }

        return new Vector2f(yawToTarget, pitchToTarget);
    }

    private float randomLerpForSloth(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    private boolean isWithinFOV(Entity target, float fov) {
        if (target == null || mc.player == null) return false;

        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d lookVec = mc.player.getLook(1.0F);

        AxisAlignedBB bb = target.getBoundingBox();
        if (bb == null) return false;

        Vector3d[] points = new Vector3d[]{
                new Vector3d(target.getPosX(), target.getPosY() + target.getEyeHeight(), target.getPosZ()),
                bb.getCenter(),
                new Vector3d(bb.minX, bb.minY, bb.minZ),
                new Vector3d(bb.maxX, bb.maxY, bb.maxZ),
                new Vector3d(bb.minX, bb.maxY, bb.minZ),
                new Vector3d(bb.maxX, bb.minY, bb.maxZ)
        };

        for (Vector3d point : points) {
            if (point == null) continue;
            Vector3d toTarget = point.subtract(eyePos).normalize();
            double dot = lookVec.dotProduct(toTarget);

            dot = Math.min(dot, 1.0);

            double angle = Math.toDegrees(Math.acos(dot));

            if (angle <= fov / 2.0) {
                return true;
            }
        }

        return false;
    }

    private float randomLerp(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private float sanitizeYaw(float yaw) {
        while (yaw > 180.0F) {
            yaw -= 360.0F;
        }
        while (yaw < -180.0F) {
            yaw += 360.0F;
        }
        return yaw;
    }

    private void spookyTimeRotation() {
        if (target == null || mc.player == null) return;

        float randomYawFactor = (float) (Math.random() * (double) 2.5F - (double) 1.5F);
        float randomPitchFactor = (float) (Math.random() * (double) 2.5F - (double) 1.0F);
        float randomThreshold = (float) (Math.random() * (double) 2.5F);
        float randomAddition = (float) (Math.random() * (double) 3.5F + (double) 2.5F);

        float neckHeight = target.getEyeHeight() - 0.3f;
        Vector3d targetPos = target.getPositionVec().add(0, neckHeight, 0);

        if (this.selected == this.target) {
            float randomOffsetX = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.1f;
            float randomOffsetZ = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.1f;
            targetPos = targetPos.add(randomOffsetX, 0, randomOffsetZ);
        }

        Vector3d vecToNeck = targetPos.subtract(mc.player.getEyePosition(1.0F));
        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vecToNeck.z, vecToNeck.x)) - 90.0);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vecToNeck.y, Math.hypot(vecToNeck.x, vecToNeck.z))));
        float yawDelta = MathHelper.wrapDegrees(yawToTarget - this.rotate.x);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - this.rotate.y);

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0E-4F), canAttack() ? 45F : 26F);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0E-4F), 7.0F);

        if (this.selected != this.target) {
            clampedPitch = Math.max(Math.abs(pitchDelta), 1.0F);
        } else {
            clampedPitch /= 3.0F;
        }

        if (Math.abs(clampedYaw - this.lastYaw) <= randomThreshold) {
            clampedYaw = this.lastYaw + randomAddition;
        }

        clampedYaw += randomYawFactor;
        clampedPitch += randomPitchFactor;
        float yaw = this.rotate.x + (yawDelta > 0.0F ? clampedYaw : -clampedYaw);
        float pitch = clamp(this.rotate.y + (pitchDelta > 0.0F ? clampedPitch : -clampedPitch), -80.0F, 70.0F);
        float gcd = SensUtil.getGCDValue();
        yaw -= (yaw - this.rotate.x) % gcd;
        pitch -= (pitch - this.rotate.y) % gcd;
        this.rotate = new Vector2f(yaw, pitch);
        this.lastYaw = clampedYaw;
        this.lastPitch = clampedPitch;
        this.selected = this.target;
    }

    public void rotationAngles(LivingEntity target) {
        if (target == null || mc.player == null) return;

        if (this.shouldAttack(target)) {
            if (settings != null && settings.options != null && settings.options.size() > 5 && settings.get(5) &&
                    player != null && player.isHandActive() && !player.isBlocking()) return;
            attackEntityAndSwing(target);
        }

        double elytraDistance = 0.0;
        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.elytraPredict != null) {
            elytraDistance = Manager.FUNCTION_MANAGER.elytraPredict.getElytraDistance(target);
        }

        if (target == null || mc.player == null) return;

        Vector3d add = new Vector3d(0, clamp(target.getPosY() - target.getHeight(), 0, target.getHeight() / 2.0f), 0);

        Vector3d targetPos = target.getPositionVec().add(add);
        Vector3d playerPos = mc.player.getEyePosition(1.0f);

        // Логика из rockstar ElytraTarget.getPos() для совместимости с перегоном
        boolean canPredict = false;
        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.elytraPredict != null) {
            canPredict = Manager.FUNCTION_MANAGER.elytraPredict.canPredict(target);
        }
        boolean isLeaving = PredictUtility.isLeaving(target);

        if (canPredict) {
            // Если цель улетает (перегон), используем логику из rockstar
            if (isLeaving) {
                // Логика перегона: смещаем позицию вперед по motion
                // В rockstar: entity.getPositionVec().add(entity.getMotion().scale(2 + Server.ping() / 50F))
                // Получаем ping цели из NetworkPlayerInfo
                double ping = 0.0;
                if (target instanceof PlayerEntity && mc.player != null && mc.player.connection != null) {
                    net.minecraft.client.network.play.NetworkPlayerInfo playerInfo =
                            mc.player.connection.getPlayerInfo(target.getUniqueID());
                    if (playerInfo != null) {
                        ping = Math.min(playerInfo.getResponseTime() / 50.0, 5.0);
                    }
                }
                double scaleFactor = 2.0 + ping;
                targetPos = targetPos.add(target.getMotion().scale(scaleFactor));
            } else {
                // Обычная логика с motionOffset
                Vector3d motionOffset = target.getMotion().mul(
                        elytraDistance,
                        elytraDistance,
                        elytraDistance
                );
                targetPos = targetPos.add(motionOffset);
            }
        }

        Vector3d pos = targetPos.subtract(playerPos);


        Vector3d direction = targetPos.subtract(mc.player.getEyePosition(1.0f));

        double rawTargetYaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0;
        float currentYaw = rotate.x;

        float deltaYaw = (float) (((rawTargetYaw - currentYaw + 180) % 360) - 180);

        float yawStep = deltaYaw;
        float targetYaw = currentYaw + yawStep;

        double horizontalDist = Math.hypot(direction.x, direction.z);
        float rawPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontalDist));

        float targetPitch = clamp(rawPitch, -90.0f, 90.0f);

        Vector2f corrected = correctRotation(targetYaw, targetPitch);

        rotate = new Vector2f(corrected.x, corrected.y);

    }

    public static Vector2f correctRotation(float yaw, float pitch) {
        if (mc.player == null) {
            return new Vector2f(yaw, pitch);
        }
        if ((yaw == -90 && pitch == 90) || yaw == -180)
            return new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);

        float gcd = SensUtil.getGCDValue();
        yaw -= yaw % gcd;
        pitch -= pitch % gcd;

        return new Vector2f(yaw, pitch);
    }

    private boolean isPlayerBetweenPoints(Vector3d playerPos, Vector3d point1, Vector3d point2) {
        double distToPoint1 = playerPos.distanceTo(point1);
        double distToPoint2 = playerPos.distanceTo(point2);
        double distBetweenPoints = point1.distanceTo(point2);

        double tolerance = 1.0;
        double totalDist = distToPoint1 + distToPoint2;

        return Math.abs(totalDist - distBetweenPoints) < tolerance;
    }

    public double attackDistance() {
        if (mc.playerController == null) {
            return distance != null ? distance.getValue().floatValue() : 3.0D;
        }
        return max(mc.playerController.extendedReach() ? 6.0D : 3.0D, distance != null ? distance.getValue().floatValue() : 3.0D);
    }

    private void findTarget(CUseEntityPacket packet) {
        if (mc.world != null && player != null && packet != null) {
            try {
                Entity entity = packet.getEntityFromWorld(mc.world);
                if (entity instanceof PlayerEntity p && p != target) {
                    target = p;
                }
            } catch (Exception e) {
                // Игнорируем ошибки при получении сущности из пакета
            }
        }
    }

    private void setRotation(final LivingEntity base, final boolean attack) {
        if (target == null || rotate == null) return;
        Vector3d vec3d = AuraUtil.getVector(target);
        if (vec3d == null) return;
        double diffX = vec3d.x;
        double diffY = vec3d.y;
        double diffZ = vec3d.z;
        float[] rotations = new float[]{(float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F, (float) (-Math.toDegrees(Math.atan2(diffY, Math.hypot(diffX, diffZ))))};
        if (rotations == null || rotations.length < 2) return;
        float deltaYaw = MathHelper.wrapDegrees(MathUtil.calculateDelta(rotations[0], rotate.x));
        float deltaPitch = MathUtil.calculateDelta(rotations[1], rotate.y);
        float limitedYaw = Math.min(Math.max(Math.abs(deltaYaw), 1.0F), 360.0F);
        float limitedPitch = Math.min(Math.max(Math.abs(deltaPitch), 1.0F), 90.0F);
        float finalYaw = rotate.x + (deltaYaw > 0.0F ? limitedYaw : -limitedYaw) + MathUtil.randomizeFloat(-1.0F, 1.0F);
        float finalPitch = clamp(rotate.y + (deltaPitch > 0.0F ? limitedPitch : -limitedPitch) + MathUtil.randomizeFloat(-1.0F, 1.0F), -90.0F, 90.0F);
        float gcd = GCDUtil.getGCDValue();
        finalYaw = (float) ((double) finalYaw - (double) (finalYaw - rotate.x) % (double) gcd);
        finalPitch = (float) ((double) finalPitch - (double) (finalPitch - rotate.y) % (double) gcd);
        rotate = new Vector2f(finalYaw, finalPitch);
    }

    private void hollyworld(final LivingEntity base, final boolean attack) {
        if (target == null || rotate == null) return;
        Vector3d vec3d = AuraUtil.getVectorHoly(target);
        if (vec3d == null) return;
        double diffX = vec3d.x;
        double diffY = vec3d.y;
        double diffZ = vec3d.z;
        float[] rotations = new float[]{(float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F, (float) (-Math.toDegrees(Math.atan2(diffY, Math.hypot(diffX, diffZ))))};
        if (rotations == null || rotations.length < 2) return;
        float deltaYaw = MathHelper.wrapDegrees(MathUtil.calculateDelta(rotations[0], rotate.x));
        float deltaPitch = MathUtil.calculateDelta(rotations[1], rotate.y);
        float limitedYaw = Math.min(Math.max(Math.abs(deltaYaw), 1.0F), 360.0F);
        float limitedPitch = Math.min(Math.max(Math.abs(deltaPitch), 1.0F), 90.0F);
        float finalYaw = rotate.x + (deltaYaw > 0.0F ? limitedYaw : -limitedYaw) + MathUtil.randomizeFloat(-1.0F, 1.0F);
        float finalPitch = clamp(rotate.y + (deltaPitch > 0.0F ? limitedPitch : -limitedPitch) + MathUtil.randomizeFloat(-1.0F, 1.0F), -90.0F, 90.0F);
        float gcd = GCDUtil.getGCDValue();
        finalYaw = (float) ((double) finalYaw - (double) (finalYaw - rotate.x) % (double) gcd);
        finalPitch = (float) ((double) finalPitch - (double) (finalPitch - rotate.y) % (double) gcd);
        rotate = new Vector2f(finalYaw, finalPitch);
    }

    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof CUseEntityPacket packet) {
            findTarget(packet);
        }
    }

    private float[] calculateRotations(LivingEntity target) {
        if (target == null || player == null) {
            return new float[]{0.0f, 0.0f};
        }
        double deltaX = target.getPosX() - player.getPosX();
        double deltaY = target.getPosY() + target.getEyeHeight() - (player.getPosY() + player.getEyeHeight());
        double deltaZ = target.getPosZ() - player.getPosZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) toDegrees(atan2(deltaZ, deltaX)) - 90.0F;
        float pitch = (float) -toDegrees(atan2(deltaY, distance));
        return new float[]{yaw, pitch};
    }

    public float wrapLerp(float step, float input, float target) {
        return input + step * wrapDegrees(target - input);
    }

    /**
     * Проверяет, есть ли препятствия на пути от таргета до точки в направлении вектора
     */
    private boolean hasObstacleOnPath(LivingEntity target, Vector3d vector, double distance) {
        if (target == null || vector == null || mc.world == null) return false;

        Vector3d targetPos = target.getPositionVec().add(0.0, target.getHeight() / 2.0, 0.0);
        Vector3d direction = vector.normalize();
        Vector3d endPoint = targetPos.add(direction.scale(distance));

        RayTraceContext context = new RayTraceContext(
                targetPos,
                endPoint,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                mc.player
        );

        RayTraceResult result = mc.world.rayTraceBlocks(context);
        return result != null && result.getType() == RayTraceResult.Type.BLOCK;
    }

    /**
     * Находит оптимальную дистанцию для вектора, чтобы избежать столкновения с препятствием
     */
    private double findOptimalDistance(LivingEntity target, Vector3d vector, double maxDistance) {
        if (target == null || vector == null) return maxDistance;

        // Бинарный поиск оптимальной дистанции
        double minDist = 0.5; // Минимальная дистанция
        double maxDist = maxDistance;
        double optimalDist = maxDistance;

        // Проверяем с шагом 0.5 блока
        for (double testDist = maxDist; testDist >= minDist; testDist -= 0.5) {
            if (!hasObstacleOnPath(target, vector, testDist)) {
                optimalDist = testDist;
                break;
            }
        }

        return optimalDist;
    }

    /**
     * Получает случайный вектор из ElytraResolver как Vector3d
     * Гарантирует, что один и тот же вектор не выпадет два раза подряд
     * Проверяет препятствия если включена настройка skipvector
     */
    private Vector3d getRandomVectorFromElytraResolver() {
        if (Manager.FUNCTION_MANAGER == null || Manager.FUNCTION_MANAGER.elytraResolver == null ||
                !Manager.FUNCTION_MANAGER.elytraResolver.state) {
            return null;
        }

        MultiBoxSetting vectorSetting = Manager.FUNCTION_MANAGER.elytraResolver.vector;
        if (vectorSetting == null) return null;
        List<String> enabledVectors = new ArrayList<>();

        if (vectorSetting.get("Вверх")) enabledVectors.add("Вверх");
        if (vectorSetting.get("Вниз")) enabledVectors.add("Вниз");
        if (vectorSetting.get("Восток")) enabledVectors.add("Восток");
        if (vectorSetting.get("Запад")) enabledVectors.add("Запад");
        if (vectorSetting.get("Юг")) enabledVectors.add("Юг");
        if (vectorSetting.get("Север")) enabledVectors.add("Север");

        if (enabledVectors.isEmpty()) {
            return null;
        }

        // Если включен только один вектор, возвращаем его (даже если он совпадает с предыдущим)
        if (enabledVectors.size() == 1 && !enabledVectors.isEmpty()) {
            String vectorName = enabledVectors.get(0);
            if (vectorName != null) {
                Vector3d vector = getVectorByName(vectorName);
                if (vector != null) {
                    lastRandomVector = vector;
                    return vector;
                }
            }
        }

        // Исключаем последний выбранный вектор из списка доступных
        List<String> availableVectors = new ArrayList<>(enabledVectors);
        if (lastRandomVector != null) {
            String lastVectorName = getVectorName(lastRandomVector);
            if (lastVectorName != null) {
                availableVectors.remove(lastVectorName);
            }
        }

        // Если после исключения не осталось векторов, используем все доступные
        if (availableVectors.isEmpty()) {
            availableVectors = enabledVectors;
        }

        // Получаем дистанцию из ElytraResolver
        double vectorDistance = 1.5;
        boolean skipCollision = false;
        if (Manager.FUNCTION_MANAGER.elytraResolver.elytradistance != null) {
            vectorDistance = Manager.FUNCTION_MANAGER.elytraResolver.elytradistance.getValue().doubleValue();
        }
        if (Manager.FUNCTION_MANAGER.elytraResolver.skipvector != null) {
            skipCollision = Manager.FUNCTION_MANAGER.elytraResolver.skipvector.get();
        }

        // Если включена проверка препятствий, ищем вектор без препятствий
        if (skipCollision && target != null) {
            List<String> vectorsWithoutObstacles = new ArrayList<>();

            // Проверяем каждый доступный вектор на наличие препятствий
            for (String vectorName : availableVectors) {
                if (vectorName != null) {
                    Vector3d testVector = getVectorByName(vectorName);
                    if (testVector != null && !hasObstacleOnPath(target, testVector, vectorDistance)) {
                        vectorsWithoutObstacles.add(vectorName);
                    }
                }
            }

            // Если есть векторы без препятствий, выбираем из них
            if (!vectorsWithoutObstacles.isEmpty() && vectorsWithoutObstacles.size() > 0) {
                try {
                    int randomIndex = ThreadLocalRandom.current().nextInt(vectorsWithoutObstacles.size());
                    if (randomIndex >= 0 && randomIndex < vectorsWithoutObstacles.size()) {
                        String randomVectorName = vectorsWithoutObstacles.get(randomIndex);
                        Vector3d vector = getVectorByName(randomVectorName);
                        lastRandomVector = vector;
                        currentVectorDistance = vectorDistance; // Используем полную дистанцию
                        return vector;
                    }
                } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                    // Игнорируем ошибки при выборе случайного индекса
                }
            }

            // Если все векторы имеют препятствия, выбираем случайный и сокращаем дистанцию
            if (availableVectors.isEmpty()) {
                return null;
            }
            try {
                int size = availableVectors.size();
                if (size <= 0) return null;
                int randomIndex = ThreadLocalRandom.current().nextInt(size);
                if (randomIndex >= 0 && randomIndex < size) {
                    String randomVectorName = availableVectors.get(randomIndex);
                    if (randomVectorName != null) {
                        Vector3d vector = getVectorByName(randomVectorName);
                        if (vector != null) {
                            // Находим оптимальную дистанцию для выбранного вектора
                            double optimalDistance = findOptimalDistance(target, vector, vectorDistance);
                            lastRandomVector = vector;
                            currentVectorDistance = optimalDistance; // Сохраняем сокращенную дистанцию
                            return vector;
                        }
                    }
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                // Игнорируем ошибки при выборе случайного индекса
            }
            return null;
        }

        // Если проверка препятствий отключена, выбираем случайный вектор
        if (availableVectors.isEmpty()) {
            return null;
        }
        try {
            int size = availableVectors.size();
            if (size <= 0) return null;
            int randomIndex = ThreadLocalRandom.current().nextInt(size);
            if (randomIndex >= 0 && randomIndex < size) {
                String randomVectorName = availableVectors.get(randomIndex);
                Vector3d vector = getVectorByName(randomVectorName);
                lastRandomVector = vector;
                currentVectorDistance = vectorDistance; // Используем полную дистанцию
                return vector;
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // Игнорируем ошибки при выборе случайного индекса
        }
        return null;
    }

    /**
     * Преобразует название вектора в Vector3d
     */
    private Vector3d getVectorByName(String vectorName) {
        if (vectorName == null) return null;
        return switch (vectorName) {
            case "Вверх" -> new Vector3d(0, 20, 0);
            case "Вниз" -> new Vector3d(0, -20, 0);
            case "Восток" -> new Vector3d(20, 0, 0);
            case "Запад" -> new Vector3d(-20, 0, 0);
            case "Юг" -> new Vector3d(0, 0, 20);
            case "Север" -> new Vector3d(0, 0, -20);
            default -> null;
        };
    }

    /**
     * Получает название вектора по его Vector3d
     */
    private String getVectorName(Vector3d vector) {
        if (vector == null) return null;

        // Сравниваем векторы с небольшой погрешностью
        double x = vector.x;
        double y = vector.y;
        double z = vector.z;

        if (Math.abs(x) < 0.1 && Math.abs(z) < 0.1) {
            if (y > 10) return "Вверх";
            if (y < -10) return "Вниз";
        } else if (Math.abs(y) < 0.1 && Math.abs(z) < 0.1) {
            if (x > 10) return "Восток";
            if (x < -10) return "Запад";
        } else if (Math.abs(x) < 0.1 && Math.abs(y) < 0.1) {
            if (z > 10) return "Юг";
            if (z < -10) return "Север";
        }

        return null;
    }

    /**
     * Проверяет расстояние от позиции игрока до целевого вектора (позиции цели)
     */
    private double getDistanceToTargetVector(LivingEntity target) {
        if (target == null) return Double.MAX_VALUE;
        Vector3d targetPos = target.getPositionVec().add(0.0, target.getHeight() / 2.0, 0.0);
        Vector3d playerPos = mc.player.getPositionVec().add(0.0, mc.player.getHeight() / 2.0, 0.0);
        return playerPos.distanceTo(targetPos);
    }

    /**
     * Проверяет, находится ли игрок в радиусе 1.5 блока от точки в направлении случайного вектора
     */
    private boolean isNearRandomVector(Vector3d randomVector, double switchDistance) {
        if (randomVector == null || target == null || mc.player == null) return false;

        // Используем currentVectorDistance если она установлена (может быть сокращена из-за препятствий)
        double actualDistance = currentVectorDistance > 0 ? currentVectorDistance : switchDistance;

        // Нормализуем вектор направления
        Vector3d direction = randomVector.normalize();

        // Вычисляем точку в направлении случайного вектора на расстоянии actualDistance от позиции цели
        Vector3d targetPos = target.getPositionVec().add(0.0, target.getHeight() / 2.0, 0.0);
        Vector3d vectorPoint = targetPos.add(direction.scale(actualDistance));

        // Вычисляем позицию игрока
        Vector3d playerPos = mc.player.getPositionVec().add(0.0, mc.player.getHeight() / 2.0, 0.0);

        // Проверяем расстояние от игрока до точки в направлении случайного вектора
        double distanceToVectorPoint = playerPos.distanceTo(vectorPoint);

        // Игрок считается близко к вектору, если он находится в радиусе 1.5 блока от точки
        return distanceToVectorPoint < VECTOR_CHECK_RADIUS;
    }


    /**
     * Использует фейерверк для ускорения на элитре
     */
    private void useFirework() {
        if (mc.player == null) return;

        if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) == -1) {
            if (mc.player.isElytraFlying()) {
                ClientUtils.sendMessage(TextFormatting.WHITE + "У вас не были найдены" + TextFormatting.RED + " фейерверки");
            }
        } else {
            if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.middleClickPearlFunction != null &&
                    Manager.FUNCTION_MANAGER.middleClickPearlFunction.legit != null &&
                    Manager.FUNCTION_MANAGER.middleClickPearlFunction.legit.get()) {
                InventoryUtils.holySwapClick(Items.ENDER_PEARL, false);
            } else {
                InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
            }
        }
    }

    /**
     * Обновляет состояние defensiveActive для пакетной заморозки
     */
    private void updateDefensiveActive() {
        if (mc.player == null || !mc.player.isElytraFlying() || Manager.FUNCTION_MANAGER == null ||
                Manager.FUNCTION_MANAGER.elytraResolver == null ||
                Manager.FUNCTION_MANAGER.elytraResolver.freezeDummy == null ||
                !Manager.FUNCTION_MANAGER.elytraResolver.freezeDummy.get() || target == null) {
            defensiveActive = false;
            freezeStartTime = 0;
            return;
        }

        double vectorSwitchDistance = 1.5;
        if (Manager.FUNCTION_MANAGER.elytraResolver.elytradistance != null) {
            vectorSwitchDistance = Manager.FUNCTION_MANAGER.elytraResolver.elytradistance.getValue().doubleValue();
        }
        long currentTime = System.currentTimeMillis();

        // Если заморозка уже активна, проверяем только таймер - заморозка должна длиться полные 350 мс
        // Заморозка продолжается независимо от позиции игрока или использования случайного вектора
        if (freezeStartTime > 0) {
            long timeSinceFreeze = currentTime - freezeStartTime;
            if (timeSinceFreeze >= FREEZE_DURATION_MS) {
                defensiveActive = false;
                freezeStartTime = 0;
                defensivePos = null;
            } else {
                defensiveActive = true;
            }
        } else if (usingRandomVector && currentRandomVector != null) {
            double actualDistance = currentVectorDistance > 0 ? currentVectorDistance : vectorSwitchDistance;
            Vector3d targetPos = target.getPositionVec().add(0.0, target.getHeight() / 2.0, 0.0);
            Vector3d direction = currentRandomVector.normalize();
            Vector3d vectorPoint = targetPos.add(direction.scale(actualDistance));

            double distanceToVector = mc.player.getPositionVec().distanceTo(vectorPoint);
            boolean shouldFreeze = distanceToVector < 2.5;

            if (shouldFreeze) {
                freezeStartTime = currentTime;
                defensivePos = mc.player.getPositionVec();
                defensiveActive = true;
            } else {
                defensiveActive = false;
            }
        } else {
            if (freezeStartTime == 0) {
                defensiveActive = false;
            }
        }

        lastDefensiveActive = defensiveActive;
    }

    /**
     * Обрабатывает десинхронизацию (пакетную заморозку) позиции на сервере
     */
    private void handleDesync(EventPacket event) {
        if (Manager.FUNCTION_MANAGER == null || Manager.FUNCTION_MANAGER.elytraResolver == null ||
                !Manager.FUNCTION_MANAGER.elytraResolver.state ||
                Manager.FUNCTION_MANAGER.elytraResolver.freezeDummy == null ||
                !Manager.FUNCTION_MANAGER.elytraResolver.freezeDummy.get()) {
            return;
        }

        boolean canUse = defensiveActive && mc != null && !mc.isSingleplayer();

        if (event.isSendPacket() && canUse) {
            IPacket<?> packet = event.getPacket();
            if (packet instanceof CPlayerPacket ||
                    packet instanceof CPlayerPacket.PositionPacket ||
                    packet instanceof CPlayerPacket.PositionRotationPacket) {
                frozenPackets.add(packet);
                event.setCancel(true);
            }
        }

        if (event.isReceivePacket() && event.getPacket() instanceof SPlayerPositionLookPacket) {
            defensiveActive = false;
        }
    }

    private void renderFrozenPosition() {
        if (defensivePos == null || mc == null || mc.getRenderManager() == null ||
                mc.getRenderManager().info == null || mc.player == null) return;

        Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
        if (cameraPos == null) return;
        Vector3d renderPos = defensivePos.subtract(cameraPos);

        AxisAlignedBB bb = new AxisAlignedBB(
                renderPos.x - 0.3, renderPos.y, renderPos.z - 0.3,
                renderPos.x + 0.3, renderPos.y + mc.player.getHeight(), renderPos.z + 0.3
        );

        int color = new java.awt.Color(255, 0, 0, 200).getRGB();

        RenderUtils.Render3D.drawBox(bb, color);
    }

    private void attackEntityAndSwing(final LivingEntity targetEntity) {
        if (targetEntity == null || mc.player == null || player == null || mc.playerController == null) return;

        if (settings != null && settings.options != null && settings.options.size() > 5 && settings.get(5) &&
                player != null && player.isHandActive() && !player.isBlocking()) return;
        if (settings != null && settings.options != null && settings.options.size() > 1 && settings.get(1) && player != null && player.isBlocking()) {
            mc.playerController.onStoppedUsingItem(player);
        }

        boolean sprint = false;
        boolean isInLiquid = player.isActualySwimming() || player.isSwimming() && player.areEyesInFluid(FluidTags.WATER) || player.areEyesInFluid(FluidTags.LAVA);

        if (sprintMode.is("Обычный")) {
            if (CEntityActionPacket.lastUpdatedSprint && !player.isInWater()) {
                player.connection.sendPacket(new CEntityActionPacket(player, CEntityActionPacket.Action.STOP_SPRINTING));
                sprint = true;
            }
        }

        ticksUntilNextAttack = 3f;
        if (!mc.player.isElytraFlying() && (rotationMode.is("LonyGrief") || rotationMode.is("Legit") || rotationMode.is("FunTime") || rotationMode.is("Spooky") || rotationMode.is("HolyWorld"))) {
            if (!rayTrace()) return;
        }

        if (this.settings != null && this.settings.options != null && this.settings.options.size() > 2 && this.settings.get(2)) {
            this.breakShield(targetEntity);
        }

        long cooldown;
        if (rotationMode.is("HolyWorld")) {
            cooldown = 450;
        } else {
            cooldown = ThreadLocalRandom.current().nextLong(480, 511);
        }

        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.syncTps != null &&
                Manager.FUNCTION_MANAGER.syncTps.state) {
            cooldown = (long) (Manager.FUNCTION_MANAGER.syncTps.getAdjustedCooldown(cooldown) * 1.1f);
        }

        this.cpsLimit = System.currentTimeMillis() + cooldown;

        mc.playerController.attackEntity(mc.player, targetEntity);
        mc.player.swingArm(Hand.MAIN_HAND);

        if (sprint && player != null && player.connection != null) {
            player.connection.sendPacket(new CEntityActionPacket(player, CEntityActionPacket.Action.START_SPRINTING));
        }
    }

    private Vector3d getAimPointForRotation(LivingEntity targetEntity) {
        if (targetEntity == null) return null;

        Vector3d base = targetEntity.getPositionVec().add(0.0, targetEntity.getHeight() / 1.2f, 0.0);

        base = base.add(
                (targetEntity.getPosX() - targetEntity.lastTickPosX),
                (targetEntity.getPosY() - targetEntity.lastTickPosY),
                (targetEntity.getPosZ() - targetEntity.lastTickPosZ)
        );

        return base;
    }

    public boolean rayTraceNone() {
        if (player == null || target == null) return false;
        return (RayTraceUtil.rayTraceEntity(player.rotationYaw, player.rotationPitch, attackDistance(), target));
    }

    public boolean rayTrace() {
        if (target == null || rotate == null) return false;
        return (RayTraceUtil.rayTraceEntity(rotate.x, rotate.y, attackDistance(), target));
    }

    private void breakShield(LivingEntity target) {
        if (target == null || mc.player == null || mc.player.inventory == null) return;

        if (target instanceof PlayerEntity entity) {
            if (target.isActiveItemStackBlocking(3) && !entity.isSpectator() && !entity.isCreative()) {
                Item item = null;

                if (mc.player.inventory != null) {
                    int inventorySize = mc.player.inventory.getSizeInventory();
                    for (int i = 0; i < 36 && i < inventorySize; ++i) {
                        try {
                            ItemStack stack = mc.player.inventory.getStackInSlot(i);
                            if (stack != null && !stack.isEmpty() && stack.getItem() instanceof AxeItem) {
                                item = stack.getItem();
                                break;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            break;
                        }
                    }
                }

                if (item != null) {
                    InventoryUtils.inventorySwapClick(item, false, InventoryUtils.UseType.ATTACK, (PlayerEntity) target, (BlockPos) null);
                }
            }
        }
    }

    private boolean shouldAttack(LivingEntity targetEntity) {
        if (targetEntity == null || mc.player == null) return false;
        return this.canAttack()
                && this.cpsLimit <= cachedCurrentTime
                && mc.player.getCooledAttackStrength(0.5f) >= 0.95f;
    }

    private boolean randomCooldownComplete() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastRandomCooldown >= randomCooldownDelay) {
            randomCooldownDelay = ThreadLocalRandom.current().nextLong(450, 540);
            lastRandomCooldown = currentTime;
            return true;
        }

        return false;
    }

    private long timeOnGround = 0;
    private boolean wasOnGround = false;
    private long lastJumpTime = 0;

    public boolean canAttack() {
        if (player == null || mc.world == null) return false;

        boolean headInWeb = false;
        boolean feetInWeb = false;
        boolean inSnow = false;
        boolean inBerryBush = false;
        double x;
        double z;

        for (x = -0.31; x <= 0.31; x += 0.31) {
            for (z = -0.31; z <= 0.31; z += 0.31) {
                for (double y = (double) player.getEyeHeight(); y >= 0.0; y -= 0.1) {
                    BlockPos headPos = new BlockPos(player.getPosX() + x, player.getPosY() + y, player.getPosZ() + z);
                    if (mc.world.getBlockState(headPos).getBlock() == Blocks.COBWEB) {
                        headInWeb = true;
                        break;
                    }
                    if (mc.world.getBlockState(headPos).getBlock() == Blocks.SWEET_BERRY_BUSH) {
                        inBerryBush = true;
                        break;
                    }
                }
            }
        }

        if (!headInWeb && !inBerryBush) {
            for (x = -0.31; x <= 0.31; x += 0.31) {
                for (z = -0.31; z <= 0.31; z += 0.31) {
                    BlockPos pos = new BlockPos(player.getPosX() + x, player.getPosY(), player.getPosZ() + z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                        feetInWeb = true;
                        break;
                    }
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.SWEET_BERRY_BUSH) {
                        inBerryBush = true;
                        break;
                    }
                }
            }
        }

        BlockPos feetPos = new BlockPos(player.getPosX(), player.getPosY(), player.getPosZ());
        if (mc.world.getBlockState(feetPos).getBlock() == Blocks.SNOW) {
            int snowLayers = mc.world.getBlockState(feetPos).get(SnowBlock.LAYERS);
            if (snowLayers == 2) {
                inSnow = true;
            }
        }

        boolean currentlyOnGround = player.isOnGround();
        if (currentlyOnGround && !wasOnGround) {
            timeOnGround = System.currentTimeMillis();
        } else if (!currentlyOnGround && wasOnGround && mc.gameSettings.keyBindJump.isKeyDown()) {
            lastJumpTime = System.currentTimeMillis();
        } else if (!currentlyOnGround) {
            timeOnGround = 0;
        }
        wasOnGround = currentlyOnGround;

        BlockPos headPosAbove = new BlockPos(player.getPosX(), player.getPosY() + 1.5, player.getPosZ());
        BlockPos headPosAbov = new BlockPos(player.getPosX(), player.getPosY() + 2, player.getPosZ());
        boolean blockAbove = mc.world.getBlockState(headPosAbove).getBlock() != Blocks.AIR;
        boolean blockAbov = mc.world.getBlockState(headPosAbov).getBlock() != Blocks.AIR;
        BlockPos blockBelow = new BlockPos(player.getPosX(), player.getPosY() - 1, player.getPosZ());
        boolean standingOnTrapdoor = mc.world.getBlockState(blockBelow).getBlock() instanceof TrapDoorBlock;
        boolean trapdoorAbove = mc.world.getBlockState(headPosAbove).getBlock() instanceof TrapDoorBlock;

        final boolean onSpace = settings != null && settings.options != null && settings.options.size() > 3 && settings.get(3)
                && mc.player != null && mc.player.isOnGround()
                && mc.gameSettings != null && !mc.gameSettings.keyBindJump.isKeyDown();

        float elytradistance1 = 0.0F;
        float elytraSpeed = 0.0F;
        if (player.isElytraFlying() && elytradist != null) {
            elytraSpeed = (float) Math.sqrt(player.getMotion().x * player.getMotion().x + player.getMotion().z * player.getMotion().z);
            elytradistance1 = this.elytradist.getValue().floatValue() - elytraSpeed * 0.1f;
        }

        float attackStrengthValue = 0.95F;
        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.auraFunction != null &&
                Manager.FUNCTION_MANAGER.auraFunction.rotationMode != null &&
                Manager.FUNCTION_MANAGER.auraFunction.rotationMode.is("Spooky")) {
            attackStrengthValue = MathUtil.randomizeFloat(0.97f, 1.0f);
        }
        float attackStrength = mc.player != null ? mc.player.getCooledAttackStrength(attackStrengthValue) : 0.0f;

        double yDiff = mc.player != null ? ((double) ((int) mc.player.getPosY()) - mc.player.getPosY()) : 0.0;
        boolean bl4 = yDiff == -0.01250004768371582;
        boolean bl5 = yDiff == -0.1875;
        float mb = MathUtil.randomizeFloat(0.001f, 0.008f);

        boolean packetCriticalsState = false;
        if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.packetCriticals != null) {
            packetCriticalsState = Manager.FUNCTION_MANAGER.packetCriticals.state;
        }
        boolean reasonForAttack = (!packetCriticalsState ? (mc.player.fallDistance > (rotationMode.is("Spooky") ? (float) (Math.random() * mb) : 0) || player.isOnLadder() || (player.isInWater() && player.areEyesInFluid(FluidTags.WATER)) || player.isRidingHorse() || (!player.isInWater() && (bl5 || bl4)) && !mc.player.isSneaking() || player.abilities.isFlying || player.isElytraFlying() || player.isPotionActive(Effects.LEVITATION) || (mc.player.isInLava() && mc.player.areEyesInFluid(FluidTags.LAVA)) || headInWeb || feetInWeb || inSnow || inBerryBush || (standingOnTrapdoor && blockAbove)) : (mc.player.fallDistance > (rotationMode.is("Spooky") ? (float) (Math.random() * mb) : 0) || player.isOnLadder() || (player.isInWater() && player.areEyesInFluid(FluidTags.WATER)) || player.isRidingHorse() || (!player.isInWater() && (bl5 || bl4)) && !mc.player.isSneaking() || player.abilities.isFlying || player.isElytraFlying() || player.isPotionActive(Effects.LEVITATION) || mc.player.isInLava() || mc.player.isInWeb() || inSnow || inBerryBush || (standingOnTrapdoor && blockAbove)));

        if (target == null || distance == null) {
            return false;
        }
        if (!(this.getDistance(target) >= (double) distance.getValue().floatValue() - elytradistance1) && player != null && !(player.getCooledAttackStrength(attackStrength) < (rotationMode.is("Spooky") ? MathUtil.randomizeFloat(0.87f, 0.91f) : 0.95F))) {
            if ((Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.freeCam != null &&
                    Manager.FUNCTION_MANAGER.freeCam.player != null) ||
                    (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.airStuck != null &&
                            Manager.FUNCTION_MANAGER.airStuck.state)) {
                return true;
            } else if (player.isInWater() && blockAbov) {
                return true;
            } else if (!mc.player.isSneaking() && blockAbove && player.isForcedDown() && !player.collidedVertically) {
                return true;
            } else if (!reasonForAttack && this.settings != null && this.settings.options != null && this.settings.options.size() > 0 && this.settings.get(0)) {
                boolean isFalling = player.getMotion().y < 0;
                boolean packetCriticalsCheck = false;
                if (Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.packetCriticals != null &&
                        Manager.FUNCTION_MANAGER.packetCriticals.state &&
                        Manager.FUNCTION_MANAGER.packetCriticals.mode != null &&
                        Manager.FUNCTION_MANAGER.packetCriticals.mode.is("Grim 1.17+")) {
                    packetCriticalsCheck = !player.isOnGround();
                } else {
                    packetCriticalsCheck = !player.isOnGround() && player.fallDistance > 0.2F;
                }
                return isFalling && (onSpace || (!inBerryBush && packetCriticalsCheck));
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private final List<LivingEntity> validTargets = new ArrayList<>();
    private long lastTargetUpdate = 0;
    private static final long TARGET_UPDATE_INTERVAL = 100;

    private LivingEntity findPriorityTarget() {
        if (mc.world == null) return null;

        String priorityTargetName = TargetCmd.getPriorityTargetName();
        if (priorityTargetName == null) return null;

        if (cachedPriorityTarget != null && priorityTargetName.equals(lastPriorityName)
                && cachedPriorityTarget.isAlive() && isValidTarget(cachedPriorityTarget)) {
            return cachedPriorityTarget;
        }

        lastPriorityName = priorityTargetName;

        try {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p != null && p.getName() != null && p.getName().getString().equalsIgnoreCase(priorityTargetName) && isValidTarget(p)) {
                    cachedPriorityTarget = p;
                    return p;
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки при получении списка игроков
        }

        cachedPriorityTarget = null;
        return null;
    }

    private LivingEntity findTarget() {
        long now = cachedCurrentTime;

        if (now - lastTargetUpdate > TARGET_UPDATE_INTERVAL || target == null || !isValidTarget(target)) {
            validTargets.clear();
            updateValidTargets();
            lastTargetUpdate = now;
        }

        if (validTargets.isEmpty()) return null;

        String priority = TargetCmd.getPriorityTargetName();
        if (priority != null) {
            for (LivingEntity e : validTargets) {
                if (e instanceof PlayerEntity p && p.getName().getString().equalsIgnoreCase(priority)) {
                    return e;
                }
            }
        }

        if (validTargets.size() > 1 && !"Дистанция".equals(sortMode.get())) {
            sortTargets(validTargets);
        }

        if (validTargets.isEmpty()) {
            return null;
        }
        return validTargets.get(0);
    }

    private void sortTargets(List<LivingEntity> targets) {
        if (targets == null || targets.isEmpty()) return;

        switch (sortMode.get()) {
            case "Умная" -> {
                targets.sort(
                        Comparator.comparingDouble((LivingEntity e) -> {
                                    if (e instanceof PlayerEntity p) {
                                        try {
                                            ItemStack chestStack = p.getItemStackFromSlot(EquipmentSlotType.CHEST);
                                            if (chestStack != null && !chestStack.isEmpty() && chestStack.getItem() == Items.ELYTRA) {
                                                return Double.MIN_VALUE;
                                            }
                                        } catch (Exception ex) {
                                            // Игнорируем ошибки при получении ItemStack
                                        }
                                        return -getEntityArmorCached(p);
                                    }
                                    return -e.getTotalArmorValue();
                                })
                                .thenComparingDouble(e -> getEntityHealth(e))
                                .thenComparingDouble(this::getDistance)
                );
            }
            case "Поле зрения" -> {
                targets.sort(Comparator.comparingDouble(this::getFOV));
            }
            case "Дистанция" -> {
                targets.sort(Comparator.comparingDouble(this::getDistance));
            }
            case "Здоровье" -> {
                targets.sort(Comparator.comparingDouble(this::getHealth));
            }
        }
    }

    private final Map<PlayerEntity, Double> armorCache = new ConcurrentHashMap<>();
    private final Map<PlayerEntity, Long> armorCacheTime = new ConcurrentHashMap<>();
    private static final long ARMOR_CACHE_TIME = 500;

    private double getEntityArmorCached(PlayerEntity player) {
        if (player == null) return 0.0;

        Long time = armorCacheTime.get(player);
        if (time != null && System.currentTimeMillis() - time < ARMOR_CACHE_TIME) {
            Double cached = armorCache.get(player);
            if (cached != null) return cached;
        }

        double armor = 0.0;
        try {
            if (player.inventory != null && player.inventory.armorInventory != null) {
                for (ItemStack stack : player.inventory.armorInventory) {
                    if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ArmorItem a) {
                        armor += a.getDamageReduceAmount();
                        armor += EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки при доступе к инвентарю
        }

        armorCache.put(player, armor);
        armorCacheTime.put(player, System.currentTimeMillis());
        return armor;
    }

    private void updateValidTargets() {
        if (player == null || mc.world == null || distance == null || rotateDistance == null || elytrarotate == null) return;

        try {
            double maxDist = distance.getValue().floatValue() + rotateDistance.getValue().floatValue() + (player.isElytraFlying() ? elytrarotate.getValue().floatValue() : 0);
            maxDist *= maxDist;

            AxisAlignedBB searchBox = player.getBoundingBox();
            if (searchBox == null) return;

            for (Entity entity : mc.world.getEntitiesWithinAABB(LivingEntity.class, searchBox.grow(maxDist))) {
                if (entity instanceof LivingEntity living && isValidTarget(living)) {
                    validTargets.add(living);
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки при поиске целей
        }
    }

    private double getHealth(LivingEntity entity) {
        if (entity == null) return Double.MAX_VALUE;
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    private double getFOV(LivingEntity entity) {
        if (entity == null || player == null) {
            return Double.MAX_VALUE;
        }
        float[] rotations = calculateRotations(entity);
        if (rotations == null || rotations.length < 2) {
            return Double.MAX_VALUE;
        }
        float targetYaw = rotations[0];
        float targetPitch = rotations[1];
        float currentYaw = player.rotationYaw;
        float currentPitch = player.rotationPitch;

        float yawDiff = Math.abs(wrapDegrees(targetYaw - currentYaw));

        float pitchDiff = Math.abs(targetPitch - currentPitch);

        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private boolean isValidTarget(final LivingEntity base) {
        if (base == null || player == null || mc.world == null) return false;
        if (base instanceof ClientPlayerEntity) return false;

        if (base.ticksExisted < 3) return false;
        if (settings == null || settings.options == null || settings.options.size() <= 4 || (!settings.get(4) && !canAttackThroughWalls(base)))
            return false;

        float elytrarotate1 = 0.0F;
        if (player.isElytraFlying()) {
            elytrarotate1 = this.elytrarotate.getValue().floatValue();
        }

        if (!player.isElytraFlying()) {
            elytrarotate1 = 0.0F;
        }

        if (mc.player.getDistanceEyePos(base) > (distance.getValue().floatValue() + rotateDistance.getValue().floatValue() + elytrarotate1))
            return false;

        if (base instanceof PlayerEntity p) {
            if (AntiBot.checkBot(base)) {
                return false;
            }
            if (base.getName() == null || mc.player == null || mc.player.getName() == null) return false;
            String playerName = base.getName().getString();
            if (targets != null && !targets.get("Друзья") && Manager.FRIEND_MANAGER != null &&
                    Manager.FRIEND_MANAGER.isFriend(playerName)) {
                return false;
            }
            if (p.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) return false;
        }

        if (targets == null) return false;

        if (base instanceof PlayerEntity && !targets.get("Игроки")) {
            return false;
        }
        if (base instanceof PlayerEntity && base.getTotalArmorValue() == 0 && !targets.get("Голые")) {
            return false;
        }
        if (base instanceof PlayerEntity && base.isInvisible() && base.getTotalArmorValue() == 0 && !targets.get("Голые невидимки")) {
            return false;
        }
        if (base instanceof PlayerEntity && base.isInvisible() && !targets.get("Невидимки")) {
            return false;
        }

        if (!targets.get("Мобы") && base instanceof MobEntity) {
            return false;
        }

        return !base.isInvulnerable() && base.isAlive() && !(base instanceof ArmorStandEntity);
    }

    private boolean canAttackThroughWalls(LivingEntity targetEntity) {
        if (targetEntity == null || player == null || mc.world == null) {
            return false;
        }
        Vector3d targetVec = targetEntity.getPositionVec().add(0.0, (double) targetEntity.getEyeHeight(), 0.0);
        Vector3d playerVec = player.getPositionVec().add(0.0, (double) player.getEyeHeight(), 0.0);
        RayTraceResult result = mc.world.rayTraceBlocks(new RayTraceContext(playerVec, targetVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
        return result != null && result.getType() == RayTraceResult.Type.MISS;
    }

    private double getDistance(LivingEntity entity) {
        if (entity == null || mc.player == null) {
            return Double.MAX_VALUE;
        }
        Vector3d vec = AuraUtil.getVector(entity);
        if (vec == null) {
            return Double.MAX_VALUE;
        }
        return vec.length();
    }

    public double getEntityArmor(PlayerEntity target) {
        if (target == null || target.inventory == null || target.inventory.armorInventory == null) return 0.0;

        double totalArmor = 0.0;
        try {
            for (ItemStack armorStack : target.inventory.armorInventory) {
                if (armorStack != null && !armorStack.isEmpty() && armorStack.getItem() instanceof ArmorItem) {
                    totalArmor += getProtectionLvl(armorStack);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // Игнорируем ошибки доступа к инвентарю
        }
        return totalArmor;
    }

    public double getEntityHealth(Entity ent) {
        if (ent == null) return 0.0;
        if (ent instanceof PlayerEntity player) {
            double armorValue = getEntityArmor(player) / 20.0;
            return (player.getHealth() + player.getAbsorptionAmount()) * armorValue;
        } else if (ent instanceof LivingEntity livingEntity) {
            return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
        }
        return 0.0;
    }

    private double getProtectionLvl(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
            return 0.0;
        }
        ArmorItem armor = (ArmorItem) stack.getItem();
        double damageReduce = armor.getDamageReduceAmount();
        if (stack.isEnchanted()) {
            damageReduce += (double) EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
        }
        return damageReduce;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            this.visualRotate = new Vector2f(player.rotationYaw, player.rotationPitch);
        }
    }

    @Override
    public void onDisable() {
        if (player != null) {
            this.rotate = new Vector2f(player.rotationYaw, player.rotationPitch);
            this.visualRotate = new Vector2f(player.rotationYaw, player.rotationPitch);
        }
        target = null;
        isSpinning = false;
        spinProgress = 0.0f;
        slothRaytraceNext = false;
        currentRandomVector = null;
        usingRandomVector = false;
        randomVectorSwitchTime = 0;
        lastRandomVector = null;
        currentVectorDistance = 0;
        defensiveActive = false;
        lastDefensiveActive = false;
        defensivePos = null;
        freezeStartTime = 0;
        if (!frozenPackets.isEmpty() && mc.player != null && mc.player.connection != null &&
                mc.player.connection.getNetworkManager() != null) {
            for (IPacket<?> p : frozenPackets) {
                mc.player.connection.getNetworkManager().sendPacketSilent(p);
            }
            frozenPackets.clear();
        }

        super.onDisable();
    }
}