package nuclear.utils.render;

import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import nuclear.control.Manager;
import nuclear.control.events.EventManager;
import nuclear.control.events.impl.player.EventAspectRatio;
import nuclear.utils.IMinecraft;
import org.joml.Vector2f;

public class ProjectUtil implements IMinecraft {

    public static Vector2f project2D(Vector3d vec) {
        return project2D(vec.x, vec.y, vec.z);
    }

    public static Vector2f project2D(double x, double y, double z) {
        Vector3d camera_pos = mc.getRenderManager().info.getProjectedView();
        Quaternion cameraRotation = mc.getRenderManager().getCameraOrientation().copy();
        cameraRotation.conjugate();

        Vector3f result3f = new Vector3f((float) (camera_pos.x - x), (float) (camera_pos.y - y), (float) (camera_pos.z - z));
        result3f.transform(cameraRotation);

        Entity renderViewEntity = mc.getRenderViewEntity();
        if (renderViewEntity instanceof PlayerEntity playerentity) {
            if (!Manager.FUNCTION_MANAGER.noRenderFunction.state || !Manager.FUNCTION_MANAGER.noRenderFunction.element.get("Тряска от урона"))
                hurtCameraEffect(playerentity, result3f);
            if (mc.gameSettings.viewBobbing) {
                calculateViewBobbing(playerentity, result3f);
            }
        }

        double fov = mc.gameRenderer.getFOVModifier(mc.getRenderManager().info, mc.getRenderPartialTicks(), true);
        float aspectRatio = getEffectiveAspectRatio();

        return calculateScreenPosition(result3f, fov, aspectRatio);
    }

    private static void calculateViewBobbing(PlayerEntity playerentity, Vector3f result3f) {
        float walked = playerentity.distanceWalkedModified;
        float f = walked - playerentity.prevDistanceWalkedModified;
        float f1 = -(walked + f * mc.getRenderPartialTicks());
        float f2 = MathHelper.lerp(mc.getRenderPartialTicks(), playerentity.prevCameraYaw, playerentity.cameraYaw);

        Quaternion quaternion = new Quaternion(Vector3f.XP, Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, true);
        quaternion.conjugate();
        result3f.transform(quaternion);

        Quaternion quaternion1 = new Quaternion(Vector3f.ZP, MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F, true);
        quaternion1.conjugate();
        result3f.transform(quaternion1);

        Vector3f bobTranslation = new Vector3f((MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F), (-Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2)), 0.0f);
        bobTranslation.setY(-bobTranslation.getY());
        result3f.add(bobTranslation);
    }

    private static void hurtCameraEffect(PlayerEntity playerentity, Vector3f result3f) {
        float partialTicks = mc.getRenderPartialTicks();
        float f = (float) playerentity.hurtTime - partialTicks;

        if (playerentity.getShouldBeDead()) {
            float f1 = Math.min((float) playerentity.deathTime + partialTicks, 20.0F);
            Quaternion quaternion1 = new Quaternion(Vector3f.ZP, 40.0F - 8000.0F / (f1 + 200.0F), true);
            quaternion1.conjugate();
            result3f.transform(quaternion1);
        }

        if (f < 0.0F) {
            return;
        }

        f = f / (float) playerentity.maxHurtTime;
        f = MathHelper.sin(f * f * f * f * (float) Math.PI);
        float f2 = playerentity.attackedAtYaw;

        Quaternion quaternion1 = new Quaternion(Vector3f.YP, -f2, true);
        quaternion1.conjugate();
        result3f.transform(quaternion1);

        Quaternion quaternion2 = new Quaternion(Vector3f.ZP, -f * 14.0F, true);
        quaternion2.conjugate();
        result3f.transform(quaternion2);

        Quaternion quaternion3 = new Quaternion(Vector3f.ZP, f2, true);
        quaternion3.conjugate();
        result3f.transform(quaternion3);
    }

    private static float getEffectiveAspectRatio() {
        float nativeAspectRatio = (float) mc.getMainWindow().getFramebufferWidth() / mc.getMainWindow().getFramebufferHeight();

        EventAspectRatio event = new EventAspectRatio(nativeAspectRatio);
        EventManager.call(event);

        float effectiveAspect = event.isCancel() ? nativeAspectRatio : event.getAspectRatio();
        return effectiveAspect / nativeAspectRatio;
    }

    private static Vector2f calculateScreenPosition(Vector3f result3f, double fov, float aspectRatio) {
        float halfHeight = mw.getScaledHeight() / 2.0F;
        float scaleFactor = halfHeight / (result3f.getZ() * (float) Math.tan(Math.toRadians(fov / 2.0F)));
        if (result3f.getZ() < 0.0F) {
            return new Vector2f(
                    (-result3f.getX() * scaleFactor / aspectRatio) + mc.getMainWindow().getScaledWidth() / 2.0F,
                    mc.getMainWindow().getScaledHeight() / 2.0F - result3f.getY() * scaleFactor
            );
        }
        return new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public static AxisAlignedBB getEntityBox(Entity entity, Vector3d vec) {
        Vector3d size = new Vector3d(entity.getBoundingBox().maxX - entity.getBoundingBox().minX, entity.getBoundingBox().maxY - entity.getBoundingBox().minY, entity.getBoundingBox().maxZ - entity.getBoundingBox().minZ);

        return new AxisAlignedBB(vec.x - size.x / 2f, vec.y, vec.z - size.z / 2f, vec.x + size.x / 2f, vec.y + size.y + (0.2F - (entity.isSneaking() && !mc.player.abilities.isFlying ? 0.1F : 0.0F)), vec.z + size.z / 2f);
    }

    public static Vector3d[] getCorners(AxisAlignedBB AABB) {
        return new Vector3d[]{new Vector3d(AABB.minX, AABB.minY, AABB.minZ), new Vector3d(AABB.minX, AABB.minY, AABB.maxZ), new Vector3d(AABB.minX, AABB.maxY, AABB.minZ), new Vector3d(AABB.minX, AABB.maxY, AABB.maxZ), new Vector3d(AABB.maxX, AABB.minY, AABB.minZ), new Vector3d(AABB.maxX, AABB.minY, AABB.maxZ), new Vector3d(AABB.maxX, AABB.maxY, AABB.minZ), new Vector3d(AABB.maxX, AABB.maxY, AABB.maxZ)};
    }

    public static boolean isInView(Entity entity) {
        if (mc.getRenderViewEntity() == null) {
            return false;
        }
        ClippingHelper frustum = mc.worldRenderer.getFrustum();
        if (frustum == null) {
            return entity.ignoreFrustumCheck;
        }
        return frustum.isBoundingBoxInFrustum(entity.getBoundingBox()) || entity.ignoreFrustumCheck;
    }


    public static float getPerspectiveScale(Vector3d worldPos, float baseSize) {
        Vector3d camera_pos = mc.getRenderManager().info.getProjectedView();
        Quaternion cameraRotation = mc.getRenderManager().getCameraOrientation().copy();
        cameraRotation.conjugate();
        Vector3f vecToTarget = new Vector3f((float) (camera_pos.x - worldPos.x), (float) (camera_pos.y - worldPos.y), (float) (camera_pos.z - worldPos.z));
        vecToTarget.transform(cameraRotation);
        float depth = vecToTarget.getZ();
        if (depth >= 0) return 0;

        double fov = mc.gameRenderer.getFOVModifier(mc.getRenderManager().info, mc.getRenderPartialTicks(), true);
        float scaleFactor = (float) mc.getMainWindow().getScaledHeight() / (2f * Math.abs(depth) * (float) Math.tan(Math.toRadians(fov / 2.0)));
        return baseSize * scaleFactor * 0.01f;
    }
}
