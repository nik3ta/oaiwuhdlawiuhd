package nuclear.module.impl.combat;

import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.client.Minecraft.player;

@Annotation(name = "TriggerBot", type = TypeList.Combat)
public class TriggerBot extends Module {


    private final BooleanSetting onlyCritical = new BooleanSetting("Только криты", true);
    private final BooleanSetting onlySpaceCritical = new BooleanSetting("Только с прыжком", false)
            .setVisible(onlyCritical::get);

    public TriggerBot() {
        addSettings(onlyCritical, onlySpaceCritical);
    }

    private long cpsLimit = 0;

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate e) {
            if (cpsLimit > System.currentTimeMillis()) {
                cpsLimit--;
            }

            if (mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY) {
                if (whenFalling() && (cpsLimit <= System.currentTimeMillis())) {
                    long cooldown = ThreadLocalRandom.current().nextLong(470, 501);
                    this.cpsLimit = System.currentTimeMillis() + cooldown;
                    if (mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY) {
                        mc.playerController.attackEntity(mc.player, ((EntityRayTraceResult) mc.objectMouseOver).getEntity());
                        mc.player.swingArm(Hand.MAIN_HAND);
                    }
                }
            }
        }
        return false;
    }

    public boolean whenFalling() {

        final boolean reasonForCancelCritical = mc.player.isPotionActive(Effects.BLINDNESS)
                || mc.player.isOnLadder()
                || (mc.player.isInWater() && player.areEyesInFluid(FluidTags.WATER))
                || mc.player.isRidingHorse()
                || mc.player.abilities.isFlying
                || mc.player.isElytraFlying();

        final boolean onSpace = onlySpaceCritical.get()
                && mc.player.isOnGround()
                && !mc.gameSettings.keyBindJump.isKeyDown();

        if (mc.player.getCooledAttackStrength(0.5f) >= 0.95f)
            return false;
        if (!reasonForCancelCritical && onlyCritical.get()) {
            return onSpace || !mc.player.isOnGround() && mc.player.fallDistance > 0.0F;
        }

        return true;
    }

}
