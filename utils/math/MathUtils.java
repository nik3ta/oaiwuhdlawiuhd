package nuclear.utils.math;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.utils.IMinecraft;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MathUtils extends MathHelper implements IMinecraft {
    public static float interpolate(float prev, float to, float value) {
        return prev + (to - prev) * value;
    }
    public static float interpolate2(float current, float old, float scale) {
        return old + (current - old) * scale;
    }

    public static double interpolate2(double start, double end, double factor) {
        return start + (end - start) * factor;
    }

    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }

    public static int interpolate(int start, int end, double factor) {
        return (int) ( start +  (end - start) * factor);
    }
    public static boolean isBlockUnder(float under) {
        if (mc.player.getPosY() < 0.0) {
            return false;
        } else {
            AxisAlignedBB aab = mc.player.getBoundingBox().offset(0.0, -under, 0.0);
            return mc.world.getCollisionShapes(mc.player, aab).toList().isEmpty();
        }
    }
    public static Vector3d interpolate(Vector3d end, Vector3d start, float multiple) {
        return new Vector3d(interpolate(end.getX(), start.getX(), (double)multiple), interpolate(end.getY(), start.getY(), (double)multiple), interpolate(end.getZ(), start.getZ(), (double)multiple));
    }

    public static double normalize(double scale, double prev, double last) {
        return prev + scale * (last - prev);
    }

    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    public static double round(double num, double increment) {
        double v = (double)Math.round(num / increment) * increment;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static float randomInRange(float min, float max) {
        return (float)(Math.random() * (double)(max - min)) + min;
    }

    public static int randomInRange(int min, int max) {
        return (int)(Math.random() * (double)(max - min)) + min;
    }

    public static void scaleElements(float xCenter, float yCenter, float scale, Runnable runnable) {
        RenderSystem.pushMatrix();
        RenderSystem.translatef(xCenter, yCenter, 0.0F);
        RenderSystem.scalef(scale, scale, 1.0F);
        RenderSystem.translatef(-xCenter, -yCenter, 0.0F);
        runnable.run();
        RenderSystem.popMatrix();
    }

    public static Vector2f rotationToEntity(Entity target) {
        Vector3d vector3d = target.getPositionVec().subtract(Minecraft.getInstance().player.getPositionVec());
        double magnitude = Math.hypot(vector3d.x, vector3d.z);
        return new Vector2f((float)Math.toDegrees(Math.atan2(vector3d.z, vector3d.x)) - 90.0F, (float)(-Math.toDegrees(Math.atan2(vector3d.y, magnitude))));
    }

    public static boolean clump(double value, double one, double too) {
        return value > one && value < too;
    }

}

