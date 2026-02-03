package nuclear.utils.render.animation;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.utils.IMinecraft;

public class AnimationMath implements IMinecraft {
    public static double deltaTime() {
        return mc.debugFPS > 0 ? (1.0000 / mc.debugFPS) : 1;
    }

    public static float fast(float end, float start, float multiple) {
        return (1 - MathHelper.clamp((float) (AnimationMath.deltaTime() * multiple), 0, 1)) * end + MathHelper.clamp((float) (AnimationMath.deltaTime() * multiple), 0, 1) * start;
    }
    public static Vector3d fast(Vector3d end, Vector3d start, float multiple) {
        return new Vector3d(
                fast((float) end.getX(), (float) start.getX(), multiple),
                fast((float) end.getY(), (float) start.getY(), multiple),
                fast((float) end.getZ(), (float) start.getZ(), multiple));
    }

    public static float lerp(float end, float start, float multiple) {
        return (float) (end + (start - end) * MathHelper.clamp(AnimationMath.deltaTime() * multiple, 0, 1));
    }

    public static double lerp(double end, double start, double multiple) {
        return (end + (start - end) * MathHelper.clamp(AnimationMath.deltaTime() * multiple, 0, 1));
    }

    // Easing functions for smooth animations
    private static float easeOutCubic(float t) {
        t = t - 1.0f;
        return t * t * t + 1.0f;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : (t - 1.0f) * (2.0f * t - 2.0f) * (2.0f * t - 2.0f) + 1.0f;
    }

    private static float easeOutQuart(float t) {
        t = t - 1.0f;
        return 1.0f - t * t * t * t;
    }

    private static float easeInOutQuart(float t) {
        if (t < 0.5f) {
            return 8.0f * t * t * t * t;
        } else {
            t = t - 1.0f;
            return 1.0f - 8.0f * t * t * t * t;
        }
    }

    private static float easeOutExpo(float t) {
        return t == 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0 * t);
    }

    /**
     * Smooth animation with ease-out-cubic easing
     * Perfect for expand/collapse animations
     */
    public static float smooth(float current, float target, float speed) {
        float delta = MathHelper.clamp((float) (deltaTime() * speed), 0, 1);
        float diff = target - current;
        float easedDelta = easeOutCubic(delta);
        return current + diff * easedDelta;
    }

    /**
     * Smooth animation with ease-in-out-cubic easing
     * Perfect for bidirectional animations
     */
    public static float smoothBidirectional(float current, float target, float speed) {
        float delta = MathHelper.clamp((float) (deltaTime() * speed), 0, 1);
        float diff = target - current;
        float easedDelta = easeInOutCubic(delta);
        return current + diff * easedDelta;
    }

    /**
     * Ultra smooth animation with ease-out-quart easing
     * Very smooth and elegant
     */
    public static float ultraSmooth(float current, float target, float speed) {
        float delta = MathHelper.clamp((float) (deltaTime() * speed), 0, 1);
        float diff = target - current;
        float easedDelta = easeOutQuart(delta);
        return current + diff * easedDelta;
    }

    public static void sizeAnimation(double width, double height, double scale) {
        GlStateManager.translated(width, height, 0);
        GlStateManager.scaled(scale, scale, scale);
        GlStateManager.translated(-width, -height, 0);
    }
}
