package nuclear.module.impl.player;

import mods.baritone.api.BaritoneAPI;
import mods.baritone.api.pathing.goals.GoalBlock;
import mods.baritone.api.pathing.goals.GoalXZ;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.utils.render.RenderUtils;
import nuclear.utils.misc.TimerUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static nuclear.ui.clickgui.Panel.getColorByName;

@Annotation(name = "AutoFarm", type = TypeList.Player, desc = "Автоматически ломает блоки вокруг игрока")
public class AutoFarm extends Module {
    private AxisAlignedBB box;
    private BlockPos pos;
    private final TimerUtil standingTimer = new TimerUtil();
    private final TimerUtil movementTimer = new TimerUtil();
    private final TimerUtil entityInteractionTimer = new TimerUtil();
    private BlockPos lastPlayerPos;
    private boolean isMoving = false;
    private BlockPos targetSnowPos;
    private boolean isMovingToEntity = false;
    private boolean isRotatingToEntity = false;
    private Boolean prevAllowBreak = null;
    
    private static final int Y_SEARCH_RANGE = 10;

    private static class WorkZone {
        final int minX, maxX, minZ, maxZ;
        final int centerX, centerZ;
        
        WorkZone(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.centerX = (minX + maxX) / 2;
            this.centerZ = (minZ + maxZ) / 2;
        }
        
        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX &&
                   pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
    }
    
    private static final WorkZone ZONE_1 = new WorkZone(4, 29, 28, 59);   // Первая зона
    private static final WorkZone ZONE_2 = new WorkZone(28, 48, 29, 63);   // Вторая зона (текущая)
    private static final WorkZone[] WORK_ZONES = {ZONE_1, ZONE_2};
    
    private WorkZone currentZone = null;
    
    private static final double PATROL_RADIUS = 10.0;
    private static final double MIN_DISTANCE_FROM_CENTER = 2.0;

    private final Vector3d targetMovePos = new Vector3d(35.5, 45, 62.5);
    private final Vector3d entityPos = new Vector3d(34.5, 46, 63.5);

    public AutoFarm() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventMotion em && isRotatingToEntity) {
            rotateToEntity(em);
        }
        if (event instanceof EventUpdate) {
            onUpdate();
        }
        return false;
    }

    public void onUpdate() {
        handleEntityInteraction();
        handleMovement();
        updateNuker();
    }

    private void handleEntityInteraction() {
        if (entityInteractionTimer.hasTimeElapsed(120000)) { // 2 минуты = 120000 миллисекунд
            if (!isMovingToEntity && !isRotatingToEntity) {
                startMovingToEntity();
            } else if (isMovingToEntity) {
                Vector3d currentPos = new Vector3d(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
                double distance = currentPos.distanceTo(targetMovePos);
                if (distance < 0.5) {
                    isMovingToEntity = false;
                    isRotatingToEntity = true;
                    movementTimer.reset();
                } else if (movementTimer.hasTimeElapsed(30000)) {
                    stopMovingToEntity();
                }
            } else if (isRotatingToEntity) {
                if (movementTimer.hasTimeElapsed(500)) {
                    interactWithEntity();
                    stopMovingToEntity();
                }
            }
        }
    }

    private void startMovingToEntity() {
        try {
            BlockPos targetBlock = new BlockPos(targetMovePos.x, targetMovePos.y, targetMovePos.z);
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(targetBlock));
            isMovingToEntity = true;
            movementTimer.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void rotateToEntity(EventMotion event) {
        float[] angles = calculateAngles(entityPos);
        event.setYaw(angles[0]);
        event.setPitch(angles[1]);
        mc.player.rotationYawHead = angles[0];
        mc.player.rotationPitchHead = angles[1];
    }
    
    private float[] calculateAngles(Vector3d to) {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        double dx = to.x - eyes.x;
        double dy = to.y - eyes.y;
        double dz = to.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(dy, dist) * 180.0 / Math.PI);
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0f, 90.0f)};
    }

    private void interactWithEntity() {
        Entity targetEntity = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Entity entity : mc.world.getAllEntities()) {
            Vector3d entityVector = new Vector3d(entity.getPosX(), entity.getPosY(), entity.getPosZ());
            double distance = entityVector.distanceTo(entityPos);
            if (distance < 1.5 && distance < minDistance) {
                targetEntity = entity;
                minDistance = distance;
            }
        }
        
        if (targetEntity != null) {
            mc.playerController.interactWithEntity(mc.player, targetEntity, Hand.MAIN_HAND);
            mc.player.swingArm(Hand.MAIN_HAND);
        }
    }

    private void stopMovingToEntity() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            isMovingToEntity = false;
            isRotatingToEntity = false;
            entityInteractionTimer.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMovement() {
        BlockPos currentPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        
        WorkZone zone = getCurrentZone(currentPos);
        if (zone == null) {
            if (isMoving) {
                stopMoving();
            }
            lastPlayerPos = currentPos;
            currentZone = null;
            return;
        }
        
        if (currentZone != zone) {
            currentZone = zone;
            if (isMoving) {
                stopMoving();
            }
        }
        
        Vector3d currentVec = new Vector3d(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        Vector3d centerVec = new Vector3d(zone.centerX + 0.5, mc.player.getPosY(), zone.centerZ + 0.5);
        double distanceFromCenter = currentVec.distanceTo(centerVec);
        
        boolean tooCloseToCenter = distanceFromCenter < MIN_DISTANCE_FROM_CENTER;
        
        if (lastPlayerPos != null && currentPos.equals(lastPlayerPos)) {
            if (isMoving) {
                if (movementTimer.hasTimeElapsed(3000)) {
                    stopMoving();
                }
            } else {
                if (tooCloseToCenter || standingTimer.hasTimeElapsed(5000)) {
                    startMovingToSnow();
                }
            }
        } else {
            if (lastPlayerPos != null && !currentPos.equals(lastPlayerPos)) {
                standingTimer.reset();
                if (isMoving) {
                    if (targetSnowPos != null) {
                        double distanceToTarget = currentPos.distanceSq(targetSnowPos);
                        if ((distanceToTarget < 4 || movementTimer.hasTimeElapsed(3000)) && !tooCloseToCenter) {
                            stopMoving();
                        } else if (tooCloseToCenter) {
                            startMovingToSnow();
                        }
                    }
                }
            }
        }
        
        lastPlayerPos = currentPos;
    }

    private void startMovingToSnow() {
        if (currentZone == null) {
            currentZone = getCurrentZone(new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()));
            if (currentZone == null) {
                return;
            }
        }
        
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        Vector3d currentVec = new Vector3d(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        Vector3d centerVec = new Vector3d(currentZone.centerX + 0.5, mc.player.getPosY(), currentZone.centerZ + 0.5);
        
        int playerY = (int) mc.player.getPosY();
        int minY = Math.max(0, playerY - Y_SEARCH_RANGE);
        int maxY = Math.min(256, playerY + Y_SEARCH_RANGE);
        
        List<BlockPos> snowBlocks = new ArrayList<>();
        
        for (int x = currentZone.minX; x <= currentZone.maxX; x++) {
            for (int z = currentZone.minZ; z <= currentZone.maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    if (validBlock(blockPos)) {
                        Vector3d snowVec = Vector3d.copyCentered(blockPos);
                        double dist = snowVec.distanceTo(centerVec);
                        if (dist <= PATROL_RADIUS && dist >= MIN_DISTANCE_FROM_CENTER) {
                            snowBlocks.add(blockPos);
                        }
                    }
                }
            }
        }
        
        if (!snowBlocks.isEmpty()) {
            snowBlocks.sort(Comparator.comparing(pos -> {
                Vector3d snowVec = Vector3d.copyCentered(pos);
                double distToPlayer = snowVec.distanceTo(currentVec);
                double distToCenter = snowVec.distanceTo(centerVec);
                double centerPref = Math.abs(distToCenter - 5.5);
                return distToPlayer + centerPref * 0.5;
            }));
            
            BlockPos targetSnow = snowBlocks.get(0);
            BlockPos targetMovePos = new BlockPos(targetSnow.getX(), targetSnow.getY(), targetSnow.getZ());
            
            try {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                        .setGoalAndPath(new GoalXZ(targetMovePos.getX(), targetMovePos.getZ()));
                isMoving = true;
                targetSnowPos = targetMovePos;
                movementTimer.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            double angle = Math.random() * 2 * Math.PI;
            double radius = MIN_DISTANCE_FROM_CENTER + Math.random() * (PATROL_RADIUS - MIN_DISTANCE_FROM_CENTER);
            int targetX = (int) (currentZone.centerX + radius * Math.cos(angle));
            int targetZ = (int) (currentZone.centerZ + radius * Math.sin(angle));
            
            targetX = Math.max(currentZone.minX, Math.min(currentZone.maxX, targetX));
            targetZ = Math.max(currentZone.minZ, Math.min(currentZone.maxZ, targetZ));
            
            try {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                        .setGoalAndPath(new GoalXZ(targetX, targetZ));
                isMoving = true;
                targetSnowPos = new BlockPos(targetX, (int) mc.player.getPosY(), targetZ);
                movementTimer.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopMoving() {
        try {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            isMoving = false;
            targetSnowPos = null;
            standingTimer.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private WorkZone getCurrentZone(BlockPos pos) {
        for (WorkZone zone : WORK_ZONES) {
            if (zone.contains(pos)) {
                return zone;
            }
        }
        return null;
    }
    
    private boolean isInSquare(BlockPos pos) {
        return getCurrentZone(pos) != null;
    }

    public void updateNuker() {
        if (mc.player == null || mc.world == null) {
            pos = null;
            box = null;
            return;
        }
        
        double radiusSq = 3.4 * 6.;
        int playerY = (int) mc.player.getPosY();
        int minY = Math.max(0, playerY - Y_SEARCH_RANGE);
        int maxY = Math.min(256, playerY + Y_SEARCH_RANGE);
        
        pos = findNearestValidBlock(radiusSq, minY, maxY);

        if (pos != null) {
            box = mc.world.getBlockState(pos).getShape(mc.world, pos).getBoundingBox().offset(pos);
            mc.playerController.onPlayerDamageBlock(pos, Direction.UP);
            mc.player.swingArm(Hand.MAIN_HAND);
        } else {
            box = null;
        }
    }
    
    private BlockPos findNearestValidBlock(double radiusSq, int minY, int maxY) {
        BlockPos playerPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        
        for (WorkZone zone : WORK_ZONES) {
            for (int x = zone.minX; x <= zone.maxX; x++) {
                for (int z = zone.minZ; z <= zone.maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos checkPos = new BlockPos(x, y, z);
                        if (validBlock(checkPos)) {
                            double distSq = playerPos.distanceSq(checkPos);
                            if (distSq <= radiusSq && distSq < nearestDistSq) {
                                nearest = checkPos;
                                nearestDistSq = distSq;
                            }
                        }
                    }
                }
            }
        }
        
        return nearest;
    }


    private boolean validBlock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.getBlock() == Blocks.SNOW || state.getBlock() == Blocks.SNOW_BLOCK;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        try {
            prevAllowBreak = BaritoneAPI.getSettings().allowBreak.value;
            BaritoneAPI.getSettings().allowBreak.value = false;
        } catch (Exception ignored) {
        }
        standingTimer.reset();
        movementTimer.reset();
        entityInteractionTimer.reset();
        lastPlayerPos = null;
        isMoving = false;
        isMovingToEntity = false;
        isRotatingToEntity = false;
        targetSnowPos = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            if (prevAllowBreak != null) {
                BaritoneAPI.getSettings().allowBreak.value = prevAllowBreak;
                prevAllowBreak = null;
            }
        } catch (Exception ignored) {
        }
        stopMoving();
        stopMovingToEntity();
    }
}
