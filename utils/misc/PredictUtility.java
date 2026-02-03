package nuclear.utils.misc;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.utils.IMinecraft;

public class PredictUtility implements IMinecraft {
	
	public static Vector3d predictElytraPos(LivingEntity player, int ticks) {
		return predictElytraPos(player, player.getPositionVec(), ticks);
	}
	
	public static Vector3d predictElytraPos(LivingEntity player, Vector3d pos, int ticks) {
	    Vector3d motion = player.getMotion();
	    
	    for (int i = 0; i < ticks; i++) {
	    	Vector3d lookVec = player.getLookVec();
	        float pitchRad = (float) Math.toRadians(player.rotationPitch);
	        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
	        double motionMag = motion.length();
	        float f1 = MathHelper.cos(pitchRad);
	        f1 = (float) (f1 * f1 * Math.min(1.0D, lookVec.length() / 0.4D));

	        motion = motion.add(0.0D, -0.08D * (-1.0D + (double) f1 * 0.75D), 0.0D);

	        if (motion.y < 0.0D && horizontalSpeed > 0.0D) {
	            double d5 = motion.y * -0.1D * f1;
	            Vector3d addVec = new Vector3d(lookVec.x * d5 / horizontalSpeed, d5, lookVec.z * d5 / horizontalSpeed);
	            motion = motion.add(addVec);
	        }

	        if (pitchRad < 0.0F && horizontalSpeed > 0.0D) {
	            double lift = motionMag * (-MathHelper.sin(pitchRad)) * 0.04D;
	            motion = motion.add(-lookVec.x * lift / horizontalSpeed, lift * 3.2D, -lookVec.z * lift / horizontalSpeed);
	        }

	        if (horizontalSpeed > 0.0D) {
	            motion = motion.add(
	                (lookVec.x / horizontalSpeed * motionMag - motion.x) * 0.1D, 
	                0.0D, 
	                (lookVec.z / horizontalSpeed * motionMag - motion.z) * 0.1D
	            );
	        }

	        motion = motion.mul(0.99D, 0.98D, 0.99D);
	        pos = pos.add(motion);
	    }
	    
	    return pos;
	}
	
	/**
	 * Проверяет, улетает ли цель (не атакует более 2000мс и летит на элитрах)
	 */
	public static boolean isLeaving(LivingEntity target) {
		if (target == null || !target.isElytraFlying()) {
			return false;
		}
		
		// Если цель не атаковала более 2000мс и летит на элитрах, значит улетает
		return target.lastSwing.finished(2000);
	}
}

