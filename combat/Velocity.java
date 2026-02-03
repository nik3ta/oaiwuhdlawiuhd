package nuclear.module.impl.combat;

import net.minecraft.network.play.server.SConfirmTransactionPacket;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.utils.misc.TimerUtil;

@Annotation(name = "Velocity", type = TypeList.Combat)
public class Velocity extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Cancel", "Cancel", "Grim", "Grim Air");
    private final BooleanSetting offPostFlagged = new BooleanSetting("Вырубить после флага", false);

    public Velocity() {
        addSettings(mode, offPostFlagged);
    }

    private int toSkip;
    private int await;
    private float countLog = 0;
    private boolean blocked = false;
    private final TimerUtil flagTimer = new TimerUtil();

    @Override
    public boolean onEvent(final Event event) {
        if (mc.player == null || mc.world == null) return false;

        if (offPostFlagged.get() && event instanceof EventPacket e && e.isReceivePacket()) {
            if (e.getPacket() instanceof SEntityVelocityPacket && mc.player.hurtTime == 9) {
                countLog += 1;
            }

            if (e.getPacket() instanceof SPlayerPositionLookPacket) {
                blocked = true;
                flagTimer.reset();
            }
        }

        if (offPostFlagged.get()) {
            if (blocked && flagTimer.hasTimeElapsed(500)) {
                blocked = false;
            }

            if (blocked) {
                return false;
            }
        }

        if (event instanceof EventPacket e && e.isReceivePacket()) {
            switch (mode.get()) {
                case "Cancel" -> {
                    if (event instanceof EventPacket ep && ep.isReceivePacket()) {
                        if (ep.getPacket() instanceof SEntityVelocityPacket velocity) {
                            if (velocity.getEntityID() == mc.player.getEntityId()) {
                                ep.setCancel(true);
                                return true;

                            }
                        }
                    }
                }

                case "Grim" -> {
                    if (e.getPacket() instanceof SEntityVelocityPacket p) {
                        if (p.getEntityID() != mc.player.getEntityId() || toSkip < 0) return false;

                        toSkip = 8;
                        event.setCancel(true);
                    }

                    if (e.getPacket() instanceof SConfirmTransactionPacket) {
                        if (toSkip < 0) toSkip++;

                        else if (toSkip > 1) {
                            toSkip--;
                            event.setCancel(true);
                        }
                    }

                    if (e.getPacket() instanceof SPlayerPositionLookPacket) toSkip = -8;
                }
                case "Grim Air" -> {
                    if (Manager.FUNCTION_MANAGER.freeCam.state || Manager.FUNCTION_MANAGER.flightFunction.state || mc.player.isOnGround() || mc.player.isElytraFlying() || mc.player.isInWater() || mc.player.isInLava()) {
                        return false;
                    }

                    if (e.getPacket() instanceof SPlayerPositionLookPacket p) {
                        mc.player.func_242277_a(new Vector3d(p.getX(), p.getY(), p.getZ()));
                        mc.player.setRawPosition(p.getX(), p.getY(), p.getZ());
                        return false;
                    }
                    if (!mc.player.isElytraFlying()) {
                        if (mc.player.fallDistance < 2F) {
                            if (e.getPacket() instanceof SConfirmTransactionPacket && mc.player.fallDistance > 0.7) {
                                e.cancel();
                            }

                            if (e.getPacket() instanceof SEntityVelocityPacket && mc.player.fallDistance > 0.7 && ((SEntityVelocityPacket) e.getPacket()).getEntityID() == mc.player.getEntityId()) {
                                e.cancel();
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private void reset() {
        toSkip = 0;
        await = 0;
        countLog = 0;
        blocked = false;
        flagTimer.reset();
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        reset();
    }
}