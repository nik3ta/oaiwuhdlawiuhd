package nuclear.utils.math;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.utils.IMinecraft;

import static net.minecraft.util.math.MathHelper.clamp;

public class AuraUtil implements IMinecraft {

    public static Vector3d getVector(LivingEntity target) {
        if (target == null) {
            return Vector3d.ZERO;
        }
        final double wHalf = target.getWidth() / 2;

        final double yExpand = clamp(target.getPosYEye() - target.getPosY(), 0, target.getHeight());
        final double xExpand = clamp(mc.player.getPosX() - target.getPosX(), -wHalf, wHalf);
        final double zExpand = clamp(mc.player.getPosZ() - target.getPosZ(), -wHalf, wHalf);

        final double yOffset = target.getPosYEye() > mc.player.getPosYEye() ? -1 : 0;

        return new Vector3d(
                target.getPosX() - mc.player.getPosX() + xExpand,
                target.getPosY() - mc.player.getPosYEye() + yExpand + yOffset,
                target.getPosZ() - mc.player.getPosZ() + zExpand
        );
    }

    public static Vector3d getVectorHoly(LivingEntity target) {
        if (target == null) {
            return Vector3d.ZERO;
        }
        double wHalf = (double)(target.getWidth() / 2.0F);
        double yExpand = MathHelper.clamp(target.getPosYEye() - target.getPosY(), (double)0.0F, (double)target.getHeight());
        double xExpand = MathHelper.clamp(mc.player.getPosX() - target.getPosX(), -wHalf, wHalf);
        double zExpand = MathHelper.clamp(mc.player.getPosZ() - target.getPosZ(), -wHalf, wHalf);
        return new Vector3d(target.getPosX() - mc.player.getPosX() + xExpand, target.getPosY() - mc.player.getPosYEye() + yExpand, target.getPosZ() - mc.player.getPosZ() + zExpand);
    }

    public static Vector3d calculateTargetVector(LivingEntity target) {
        if (target == null) {
            return Vector3d.ZERO;
        }
        Vector3d targetEyePosition = target.getPositionVec().add(0, target.getEyeHeight() - 0.24, 0);
        return targetEyePosition.subtract(mc.player.getEyePosition(1.0F));
    }
}
