package nuclear.module.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

import java.util.Comparator;

@Annotation(name = "CrystalOptimizer", type = TypeList.Combat, desc = "Взрывает кристаллы на которые навёлся")
public class CrystalOptimizer extends Module {
    private long lastAttackTime = 0;
    private int attackTicks = 0;
    private final int BASE_ATTACK_TICKS = 1;

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            handleUpdate();
        }
        return false;
    }

    @Override
    public void onDisable() {
        lastAttackTime = 0;
        attackTicks = 0;
    }

    private void handleUpdate() {
        boolean holdingCrystal = mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL ||
                mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL;
        if (!holdingCrystal || mc.player.getCooledAttackStrength(1.0F) < 1.0F) {
            return;
        }

        float ticksPerAttack = BASE_ATTACK_TICKS;

        attackTicks++;
        if (attackTicks < (int) Math.ceil(ticksPerAttack)) {
            return;
        }

        Entity crystal = mc.world.getEntitiesWithinAABB(EnderCrystalEntity.class,
                        mc.player.getBoundingBox().grow(mc.playerController.getBlockReachDistance()))
                .stream()
                .filter(this::canBeSeen)
                .min(Comparator.comparingDouble(e -> mc.player.getDistanceSq(e)))
                .orElse(null);

        if (crystal != null) {
            mc.playerController.attackEntity(mc.player, crystal);
            mc.player.swingArm(Hand.MAIN_HAND);
            lastAttackTime = System.currentTimeMillis();
            attackTicks = 0;
        }
    }

    private boolean canBeSeen(Entity entity) {
        double reach = mc.playerController.getBlockReachDistance();
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        AxisAlignedBB bb = entity.getBoundingBox().grow(0.2);
        
        Vector3d lookVec = mc.player.getLook(1.0F);
        Vector3d endPos = eyePos.add(lookVec.scale(reach));
        
        java.util.Optional<Vector3d> intersection = bb.rayTrace(eyePos, endPos);
        if (intersection.isPresent()) {
            Vector3d hitPoint = intersection.get();
            double hitDist = eyePos.squareDistanceTo(hitPoint);
            
            RayTraceContext context = new RayTraceContext(
                    eyePos, hitPoint,
                    RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE,
                    mc.player
            );
            RayTraceResult blockResult = mc.world.rayTraceBlocks(context);
            
            if (blockResult.getType() == RayTraceResult.Type.MISS) {
                return true;
            }
            
            if (blockResult.getType() == RayTraceResult.Type.BLOCK) {
                double blockDist = eyePos.squareDistanceTo(blockResult.getHitVec());
                if (hitDist < blockDist + 0.1) {
                    return true;
                }
            }
        }
        
        Vector3d center = entity.getPositionVec().add(0, entity.getHeight() / 2.0, 0);
        double halfWidth = (bb.maxX - bb.minX) / 2.0;
        double halfDepth = (bb.maxZ - bb.minZ) / 2.0;
        double halfHeight = (bb.maxY - bb.minY) / 2.0;
        
        Vector3d[] checkPoints = {
                center,
                new Vector3d(bb.minX + halfWidth, bb.minY, bb.minZ + halfDepth), // Низ
                new Vector3d(bb.minX + halfWidth, bb.maxY, bb.minZ + halfDepth), // Верх
                new Vector3d(bb.minX, bb.minY + halfHeight, bb.minZ + halfDepth), // Левая сторона
                new Vector3d(bb.maxX, bb.minY + halfHeight, bb.minZ + halfDepth), // Правая сторона
                new Vector3d(bb.minX + halfWidth, bb.minY + halfHeight, bb.minZ), // Передняя сторона
                new Vector3d(bb.minX + halfWidth, bb.minY + halfHeight, bb.maxZ), // Задняя сторона
                new Vector3d(bb.minX, bb.minY, bb.minZ),
                new Vector3d(bb.maxX, bb.minY, bb.minZ),
                new Vector3d(bb.minX, bb.maxY, bb.minZ),
                new Vector3d(bb.maxX, bb.maxY, bb.minZ),
                new Vector3d(bb.minX, bb.minY, bb.maxZ),
                new Vector3d(bb.maxX, bb.minY, bb.maxZ),
                new Vector3d(bb.minX, bb.maxY, bb.maxZ),
                new Vector3d(bb.maxX, bb.maxY, bb.maxZ)
        };
        
        for (Vector3d point : checkPoints) {
            double distSq = eyePos.squareDistanceTo(point);
            if (distSq > reach * reach) {
                continue;
            }
            
            RayTraceContext context = new RayTraceContext(
                    eyePos, point,
                    RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE,
                    mc.player
            );
            RayTraceResult result = mc.world.rayTraceBlocks(context);
            if (result.getType() == RayTraceResult.Type.MISS) {
                return true;
            }
        }
        
        return false;
    }
}
