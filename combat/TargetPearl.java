package nuclear.module.impl.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventInput;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BindSetting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.world.InventoryUtils;

import java.util.List;

@Annotation(name = "TargetPearl", type = TypeList.Combat, desc = "Кидает перл точно за перлом врага (через всё)")
@Getter
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetPearl extends Module {
    private final BooleanSetting onlyTarget = new BooleanSetting("Только за таргетом", false);
    private final SliderSetting distance = new SliderSetting("Мин дистанция", 10, 8, 20, 1);
    private final TimerUtil timerUtil = new TimerUtil();
    private final InventoryUtils.Hands handUtil = new InventoryUtils.Hands();

    private EnderPearlEntity targetPearl = null;
    private Vector3d cachedLanding = null;
    private long lastPearlScan = 0;
    private long lastThrowTime = 0;
    private long delay = 0;
    private long rotationHoldUntil = 0;
    private boolean isThrowing = false;
    public Vector2f server = null;
    
    private Entity rememberedTarget = null;
    private long rememberedTargetTime = 0;
    private static final long TARGET_MEMORY_DURATION = 10000;

    private static final int PEARL_SCAN_INTERVAL = 70;
    private static final int THROW_COOLDOWN = 2000;
    private static final int MAX_SIM_TICKS = 160;
    private static final double MAX_DISTANCE_FROM_TARGET = 1.5;
    private static final double MAX_ACCEPTABLE_ERROR = 2.0;

    public TargetPearl() {
        addSettings(onlyTarget, distance);
    }

    public boolean check() {
        return state && isThrowing && server != null;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event == null || mc.player == null || mc.world == null || mc.player.isElytraFlying()) return false;

        if (rotationHoldUntil > 0 && System.currentTimeMillis() >= rotationHoldUntil) {
            isThrowing = false;
            server = null;
            rotationHoldUntil = 0;
        }

        if (event instanceof EventInput e && check()) {
            MoveUtil.fixMovement(e, server.x);
        }

        if (event instanceof EventUpdate) {
            tryThrow();
        }

        return false;
    }

    private void tryThrow() {
        if (System.currentTimeMillis() - lastThrowTime < THROW_COOLDOWN) return;
        if (!canThrow()) return;

        updateTargetPearl();
        if (cachedLanding == null) return;

        float[] rot = calculateYawPitch(cachedLanding);
        if (rot == null) {
            rot = findAnyWorkingRotation(cachedLanding);
            if (rot == null) return;
        }

        Vector3d predicted = simulateTrajectory(rot[0], rot[1]);
        if (predicted != null) {
            double distToTarget = cachedLanding.distanceTo(predicted);
            Vector3d eye = mc.player.getEyePosition(1.0f);
            double distToCached = eye.distanceTo(cachedLanding);
            double heightDiff = cachedLanding.y - eye.y;
            boolean isHighTarget = heightDiff > 5.0;
            
            // Для дальних дистанций и случаев с препятствиями увеличиваем допустимую погрешность
            double maxAllowedError = distToCached > 60 
                    ? MAX_DISTANCE_FROM_TARGET * 2.0 
                    : MAX_DISTANCE_FROM_TARGET * 1.5;
            
            // Для высоких целей увеличиваем допустимую погрешность
            if (isHighTarget) {
                maxAllowedError = MAX_DISTANCE_FROM_TARGET * 3.0;
            }
            
            // Проверяем, есть ли препятствие между игроком и целью
            if (hasObstacleBetween(eye, cachedLanding)) {
                maxAllowedError = isHighTarget 
                        ? MAX_DISTANCE_FROM_TARGET * 4.0 
                        : MAX_DISTANCE_FROM_TARGET * 2.5;
            }
            
            if (distToTarget > maxAllowedError) {
                rot = findAnyWorkingRotation(cachedLanding);
                if (rot == null) return;
            }
        }

        isThrowing = true;
        server = new Vector2f(rot[0], rot[1]);
        sendPlayerRotationPacket(rot[0], rot[1], mc.player.isOnGround());

        if (!mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL) && InventoryUtils.getItemSlot(Items.ENDER_PEARL) != -1) {
            if (Manager.FUNCTION_MANAGER.middleClickPearlFunction.legit.get()) {
                InventoryUtils.holySwapClick(Items.ENDER_PEARL, false);
            } else {
                if (ClientUtils.isConnectedToServer("funtime") || ClientUtils.isConnectedToServer("spooky")) {
                    handUtil.handleItemChange(System.currentTimeMillis() - this.delay > 200L);

                    int hbSlot = findItem(Items.ENDER_PEARL, true);
                    int invSlot = findItem(Items.ENDER_PEARL, false);

                    if (Items.ENDER_PEARL != null) {
                        int slot = findAndTrowItem(hbSlot, invSlot);
                        if (slot != -1 && slot > 8) {
                            mc.playerController.pickItem(slot);
                        }
                    }
                    useItem(Hand.MAIN_HAND);

                } else {
                    InventoryUtils.inventorySwapClick(Items.ENDER_PEARL, false);
                    useItem(Hand.MAIN_HAND);
                }
            }
            timerUtil.reset();
            lastThrowTime = System.currentTimeMillis();
        }

        if (Manager.FUNCTION_MANAGER.middleClickPearlFunction.legit.get()) {
            rotationHoldUntil = System.currentTimeMillis() + 50L;
        } else {
            isThrowing = false;
            server = null;
            rotationHoldUntil = 0;
        }
    }

    private boolean canThrow() {
        return !mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL);
    }

    private void updateTargetPearl() {
        long now = System.currentTimeMillis();
        if (now - lastPearlScan < PEARL_SCAN_INTERVAL) return;

        lastPearlScan = now;
        cachedLanding = null;
        targetPearl = findBestPearl();

        if (targetPearl != null && targetPearl.isAlive()) {
            cachedLanding = predictLanding(targetPearl);
            if (cachedLanding != null) {
                BlockPos pos = new BlockPos(cachedLanding);
                // Проверяем только что блок под точкой приземления не пустой (перл должен приземлиться на блок)
                // Но не блокируем если есть препятствие - алгоритм найдет обходной путь
                if (mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()).isEmpty()) {
                    // Проверяем еще один блок ниже на случай если перл приземлится ниже
                    if (mc.world.getBlockState(pos.down(2)).getCollisionShape(mc.world, pos.down(2)).isEmpty()) {
                        cachedLanding = null;
                    }
                }
            }
        }
    }

    private EnderPearlEntity findBestPearl() {
        List<Entity> entities = mc.world.getEntitiesWithinAABBExcludingEntity(
                mc.player, mc.player.getBoundingBox().grow(130));

        EnderPearlEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double minDist = distance.getValue().doubleValue();
        
        Entity targetToUse = null;
        if (onlyTarget.get()) {
            Entity currentAuraTarget = Manager.FUNCTION_MANAGER.auraFunction.getTarget();
            long currentTime = System.currentTimeMillis();
            
            // Если есть текущий таргет из AttackAura, запоминаем его
            if (currentAuraTarget != null && currentAuraTarget.isAlive()) {
                rememberedTarget = currentAuraTarget;
                rememberedTargetTime = currentTime;
                targetToUse = currentAuraTarget;
            } 
            // Если нет текущего таргета, но есть запомненный и он еще валиден
            else if (rememberedTarget != null) {
                // Проверяем, что запомненный таргет еще жив и валиден
                if (!rememberedTarget.isAlive() || rememberedTarget.removed) {
                    // Таргет умер или удален, сбрасываем
                    rememberedTarget = null;
                    rememberedTargetTime = 0;
                } else {
                    long timeSinceRemembered = currentTime - rememberedTargetTime;
                    if (timeSinceRemembered < TARGET_MEMORY_DURATION) {
                        targetToUse = rememberedTarget;
                    } else {
                        // Время истекло, сбрасываем запомненного таргета
                        rememberedTarget = null;
                        rememberedTargetTime = 0;
                    }
                }
            }
        }

        for (Entity e : entities) {
            if (!(e instanceof EnderPearlEntity pearl) || !pearl.isAlive()) continue;
            if (onlyTarget.get()) {
                if (targetToUse == null) continue;
                if (pearl.getShooter() == null || !pearl.getShooter().equals(targetToUse)) continue;
            }

            Vector3d landing = predictLanding(pearl);
            if (landing == null) continue;

            double dist = mc.player.getPositionVec().distanceTo(landing);
            // Увеличиваем максимальное расстояние до 120 блоков для дальних перлов
            if (dist >= minDist && dist <= 120 && dist < bestDist) {
                best = pearl;
                bestDist = dist;
            }
        }
        return best;
    }

    private Vector3d predictLanding(EnderPearlEntity pearl) {
        Vector3d pos = pearl.getPositionVec();
        Vector3d vel = pearl.getMotion();

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            Vector3d next = pos.add(vel);
            vel = vel.scale(0.99).subtract(0, 0.03, 0);

            if (next.y <= 0) {
                return snapToBlockCenter(next);
            }

            BlockPos bp = new BlockPos(next);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return snapToBlockCenter(next);
            }
            pos = next;
        }
        return null;
    }

    private Vector3d snapToBlockCenter(Vector3d vec) {
        return new Vector3d(
                MathHelper.floor(vec.x) + 0.5,
                MathHelper.floor(vec.y),
                MathHelper.floor(vec.z) + 0.5
        );
    }

    private boolean hasObstacleBetween(Vector3d start, Vector3d end) {
        Vector3d direction = end.subtract(start);
        double distance = direction.length();
        Vector3d normalized = direction.normalize();
        
        int steps = (int) (distance / 0.5) + 1;
        for (int i = 1; i < steps; i++) {
            Vector3d checkPos = start.add(normalized.scale(i * 0.5));
            BlockPos bp = new BlockPos(checkPos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private float[] calculateYawPitch(Vector3d target) {
        Vector3d eye = mc.player.getEyePosition(1.0f);
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

        double dist = eye.distanceTo(target);
        double heightDiff = target.y - eye.y;
        boolean isHighTarget = heightDiff > 5.0; // Цель значительно выше игрока
        
        // Для высоких целей используем более широкий диапазон pitch
        float maxPitch = isHighTarget ? 89.0f : 85.0f;
        float minPitch = dist > 60 ? -50.0f : -30.0f;
        if (isHighTarget) {
            minPitch = -80.0f; // Для высоких целей пробуем более низкие углы
        }
        float step = dist > 60 ? 0.5f : 0.42f;
        if (isHighTarget) {
            step = 0.3f; // Более мелкий шаг для высоких целей
        }

        float bestPitch = 0;
        int bestTicks = Integer.MAX_VALUE;
        double bestError = Double.MAX_VALUE;
        double maxError = isHighTarget ? MAX_ACCEPTABLE_ERROR * 2.0 : MAX_ACCEPTABLE_ERROR;

        for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
            SimulationResult res = simulateWithTicks(yaw, pitch, target);
            if (res != null && res.error <= maxError) {
                if (res.ticks < bestTicks || (res.ticks == bestTicks && res.error < bestError)) {
                    bestTicks = res.ticks;
                    bestPitch = pitch;
                    bestError = res.error;
                }
            }
        }

        if (bestTicks != Integer.MAX_VALUE) {
            return new float[]{yaw, MathHelper.clamp(bestPitch, -90f, 90f)};
        }
        return null;
    }

    private float[] findAnyWorkingRotation(Vector3d target) {
        Vector3d eye = mc.player.getEyePosition(1.0f);
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float baseYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        
        double dist = eye.distanceTo(target);
        double heightDiff = target.y - eye.y;
        boolean isHighTarget = heightDiff > 5.0;
        
        // Для высоких целей используем более широкий диапазон
        float maxPitch = isHighTarget ? 89.0f : 85.0f;
        float minPitch = dist > 60 ? -70.0f : -50.0f;
        if (isHighTarget) {
            minPitch = -85.0f; // Для высоких целей пробуем очень низкие углы
        }
        float step = dist > 60 ? 1.2f : 1.5f;
        if (isHighTarget) {
            step = 1.0f; // Более мелкий шаг для высоких целей
        }
        float yawRange = dist > 60 ? 30.0f : 20.0f;
        if (isHighTarget) {
            yawRange = 40.0f; // Больший диапазон yaw для высоких целей
        }
        float yawStep = dist > 60 ? 3.0f : 2.5f;
        if (isHighTarget) {
            yawStep = 2.0f;
        }

        double baseMaxError = dist > 60 ? MAX_DISTANCE_FROM_TARGET * 2.0 : MAX_DISTANCE_FROM_TARGET * 1.5;
        if (isHighTarget) {
            baseMaxError = MAX_DISTANCE_FROM_TARGET * 3.0; // Больше погрешность для высоких целей
        }

        // Сначала пробуем точный yaw
        for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
            Vector3d landing = simulateTrajectory(baseYaw, pitch);
            if (landing != null) {
                double error = target.distanceTo(landing);
                if (error <= baseMaxError) {
                    return new float[]{baseYaw, MathHelper.clamp(pitch, -90f, 90f)};
                }
            }
        }

        // Пробуем разные yaw для обхода препятствий
        for (float yawOffset = -yawRange; yawOffset <= yawRange; yawOffset += yawStep) {
            if (yawOffset == 0) continue;
            float yaw = baseYaw + yawOffset;
            for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
                Vector3d landing = simulateTrajectory(yaw, pitch);
                if (landing != null) {
                    double error = target.distanceTo(landing);
                    if (error <= baseMaxError) {
                        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
                    }
                }
            }
        }
        
        // Для высоких целей пробуем очень высокие углы
        if (isHighTarget) {
            for (float pitch = 89.0f; pitch >= 75.0f; pitch -= 1.5f) {
                for (float yawOffset = -yawRange; yawOffset <= yawRange; yawOffset += yawStep) {
                    float yaw = baseYaw + yawOffset;
                    Vector3d landing = simulateTrajectory(yaw, pitch);
                    if (landing != null) {
                        double error = target.distanceTo(landing);
                        if (error <= baseMaxError * 1.5) {
                            return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
                        }
                    }
                }
            }
        }
        
        // Последняя попытка - пробуем очень высокие углы для перелета препятствий
        for (float pitch = maxPitch; pitch >= 70.0f; pitch -= 2.0f) {
            for (float yawOffset = -yawRange; yawOffset <= yawRange; yawOffset += yawStep * 2) {
                float yaw = baseYaw + yawOffset;
                Vector3d landing = simulateTrajectory(yaw, pitch);
                if (landing != null) {
                    double error = target.distanceTo(landing);
                    double maxError = isHighTarget ? MAX_DISTANCE_FROM_TARGET * 4.0 : MAX_DISTANCE_FROM_TARGET * 2.5;
                    if (error <= maxError) {
                        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
                    }
                }
            }
        }
        
        return null;
    }

    private static class SimulationResult {
        final double error;
        final int ticks;
        SimulationResult(double error, int ticks) {
            this.error = error;
            this.ticks = ticks;
        }
    }

    private SimulationResult simulateWithTicks(float yaw, float pitch, Vector3d target) {
        Vector3d pos = getThrowPos(yaw, pitch);
        Vector3d motion = getThrowMotion(yaw, pitch);

        for (int tick = 0; tick < MAX_SIM_TICKS; tick++) {
            pos = pos.add(motion);
            motion = motion.scale(0.99).subtract(0, 0.03, 0);

            if (pos.y <= 0) {
                return new SimulationResult(snapToBlockCenter(pos).distanceTo(target), tick + 1);
            }

            BlockPos bp = new BlockPos(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return new SimulationResult(snapToBlockCenter(pos).distanceTo(target), tick + 1);
            }
        }
        return null;
    }

    private Vector3d simulateTrajectory(float yaw, float pitch) {
        Vector3d pos = getThrowPos(yaw, pitch);
        Vector3d motion = getThrowMotion(yaw, pitch);

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            pos = pos.add(motion);
            motion = motion.scale(0.99).subtract(0, 0.03, 0);

            if (pos.y <= 0) {
                return snapToBlockCenter(pos);
            }

            BlockPos bp = new BlockPos(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return snapToBlockCenter(pos);
            }
        }
        return null;
    }

    private Vector3d getThrowPos(float yaw, float pitch) {
        float yr = (float) Math.toRadians(yaw);
        double x = mc.player.getPosX() - MathHelper.cos(yr) * 0.16;
        double y = mc.player.getPosY() + mc.player.getEyeHeight() - 0.1;
        double z = mc.player.getPosZ() - MathHelper.sin(yr) * 0.16;
        return new Vector3d(x, y, z);
    }

    private Vector3d getThrowMotion(float yaw, float pitch) {
        double v = 1.5;
        float yr = (float) Math.toRadians(yaw);
        float pr = (float) Math.toRadians(pitch);
        double vx = -MathHelper.sin(yr) * MathHelper.cos(pr) * v;
        double vy = -MathHelper.sin(pr) * v;
        double vz = MathHelper.cos(yr) * MathHelper.cos(pr) * v;
        
        double playerVy = mc.player.getMotion().y;
        vy += playerVy;
        
        return new Vector3d(vx, vy, vz);
    }

    private void sendPlayerRotationPacket(float yaw, float pitch, boolean onGround) {
        if (mc.getConnection() != null) {
            mc.getConnection().sendPacket(new CPlayerPacket.RotationPacket(yaw, pitch, onGround));
        }
    }

    private int findItem(Item item, boolean hotbarOnly) {
        for (int i = hotbarOnly ? 0 : 9; i < (hotbarOnly ? 9 : 36); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findAndTrowItem(int hbSlot, int invSlot) {
        if (hbSlot != -1) {
            this.handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return hbSlot;
        }
        if (invSlot != -1) {
            handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            mc.playerController.pickItem(invSlot);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return invSlot;
        }
        return -1;
    }

    private void useItem(Hand hand) {
        if (mc.getConnection() != null) {
            mc.getConnection().sendPacket(new CPlayerTryUseItemPacket(hand));
            mc.player.swingArm(hand);
            var cooldown = Manager.FUNCTION_MANAGER.gappleCooldownFunction;
            if (cooldown != null && cooldown.state) {
                cooldown.onItemUsed(Items.ENDER_PEARL);
            }
        }
    }

    @Override
    protected void onDisable() {
        isThrowing = false;
        targetPearl = null;
        cachedLanding = null;
        server = null;
        rotationHoldUntil = 0;
        timerUtil.reset();
        rememberedTarget = null;
        rememberedTargetTime = 0;
        super.onDisable();
    }
}