package nuclear.module.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.UseAction;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventNoSlow;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.move.MoveUtil;

@Annotation(name = "NoSlow", type = TypeList.Movement, desc = "Уберает замедление при исп предметов")
public class NoSlow extends Module {
    public ModeSetting mode = new ModeSetting("Мод", "Grim", "Matrix", "Grim", "FunTime", "ReallyWorld", "Spooky");
    public final SliderSetting vanillaSpeed = new SliderSetting("Скорость", 0.6F, 0.1F, 1F, 0.05F).setVisible(() -> mode.is("Vanilla"));
    public final BooleanSetting OnlyJump = new BooleanSetting("Только на земле", false).setVisible(() -> !mode.is("FunTime"));
    public static int ticks = 0;
    private static int cycleCounter = 0;

    public NoSlow() {
        addSettings(mode, vanillaSpeed, OnlyJump);
    }

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventUpdate e) {
            eventUpdate(e);
        }
        if (Minecraft.player.isElytraFlying()) return false;
        if (OnlyJump.get() && !Minecraft.player.isOnGround()) return false;
        if (event instanceof EventNoSlow eventNoSlow) {
            handleEventUpdate(eventNoSlow);
        }
        return false;
    }

    public void eventUpdate(EventUpdate event) {
        if ((mode.is("ReallyWorld") || mode.is("Spooky")) && mc.player != null && !mc.player.isElytraFlying()) {
            if (mc.player.isHandActive()) {
                ticks++;
            } else {
                ticks = 0;
                cycleCounter = 0;
            }
        }
    }

    private void handleEventUpdate(EventNoSlow eventNoSlow) {
        if (Minecraft.player.isHandActive()) {
            switch (mode.get()) {
                case "Vanilla" -> handleVanillaMode(eventNoSlow);
                case "Grim" -> handleGrimACMode(eventNoSlow);
                case "Matrix" -> handleMatrixMode(eventNoSlow);
                case "FunTime" -> handleFunTimeMode(eventNoSlow);
                case "ReallyWorld" -> handlegrim(eventNoSlow);
                case "Spooky" -> handleSpookyTime(eventNoSlow);
            }
        }
    }

    private void handlegrim(EventNoSlow e) {
        int[] thresholds = {2, 3, 3};
        int threshold = thresholds[cycleCounter % 3];
        if (ticks >= threshold) {
            e.setCancel(true);
            ticks = 0;
            cycleCounter++;
        }
    }

    private void handleSpookyTime(EventNoSlow e) {
        int[] thresholds = {2, 2, 2};
        int threshold = thresholds[cycleCounter % 2];
        if (ticks >= threshold) {
            e.setCancel(true);
            ticks = 0;
            cycleCounter++;
        }
    }

    public void handleFunTimeMode(EventNoSlow eventNoSlow) {
        if (mc.player.isOnGround() && (mc.player.getHeldItemMainhand().getItem() instanceof CrossbowItem)) {
            eventNoSlow.cancel();
        }
    }

    private void handleVanillaMode(EventNoSlow eventNoSlow) {
        eventNoSlow.setCancel(true);
        float speedMultiplier = vanillaSpeed.getValue().floatValue();
        Minecraft.player.motion.x *= speedMultiplier;
        Minecraft.player.motion.z *= speedMultiplier;
    }

    private void handleMatrixMode(EventNoSlow eventNoSlow) {
        boolean isFalling = (double) Minecraft.player.fallDistance > 0.725;
        float speedMultiplier;
        eventNoSlow.setCancel(true);

        if (Minecraft.player.isOnGround() && !Minecraft.player.movementInput.jump) {
            if (Minecraft.player.ticksExisted % 2 == 0) {
                boolean isNotStrafing = Minecraft.player.moveStrafing == 0.0F;
                speedMultiplier = isNotStrafing ? 0.5F : 0.4F;

                speedMultiplier *= 2;

                Minecraft.player.motion.x *= speedMultiplier;
                Minecraft.player.motion.z *= speedMultiplier;
            }
        } else if (isFalling) {
            boolean isVeryFastFalling = (double) Minecraft.player.fallDistance > 1.4;
            speedMultiplier = isVeryFastFalling ? 0.95F : 0.97F;

            if (Minecraft.player.motion.y < -0.5) {
                speedMultiplier *= 0.9F;
            } else if (Minecraft.player.motion.y < -0.2) {
                speedMultiplier *= 0.95F;
            }

            Minecraft.player.motion.x *= speedMultiplier;
            Minecraft.player.motion.z *= speedMultiplier;
        }

        if (Minecraft.player.isInWater()) {
            Minecraft.player.motion.x *= 0.8F;
            Minecraft.player.motion.z *= 0.8F;
        }

        if (Minecraft.player.isSneaking()) {
            Minecraft.player.motion.x *= 0.5F;
            Minecraft.player.motion.z *= 0.5F;
        }

        if (Minecraft.player.ticksExisted % 100 == 0) {
            Minecraft.player.motion.x *= 1.0F;
            Minecraft.player.motion.z *= 1.0F;
        }
    }

    private void handleGrimACMode(EventNoSlow noSlow) {
        if (Minecraft.player.getHeldItemOffhand().getUseAction() == UseAction.BLOCK && Minecraft.player.getActiveHand() == Hand.MAIN_HAND || Minecraft.player.getHeldItemOffhand().getUseAction() == UseAction.EAT && Minecraft.player.getActiveHand() == Hand.MAIN_HAND) {
            return;
        }

        if (Minecraft.player.getActiveHand() == Hand.MAIN_HAND) {
            Minecraft.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
            noSlow.setCancel(true);
            return;
        }

        noSlow.setCancel(true);
        sendItemChangePacket();
    }

    private void sendItemChangePacket() {
        if (MoveUtil.isMoving()) {
            Minecraft.player.connection.sendPacket(new CHeldItemChangePacket((Minecraft.player.inventory.currentItem % 8 + 1)));
            Minecraft.player.connection.sendPacket(new CHeldItemChangePacket(Minecraft.player.inventory.currentItem));
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
    }
}