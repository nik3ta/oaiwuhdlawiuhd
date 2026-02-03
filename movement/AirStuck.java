package nuclear.module.impl.movement;

import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.player.EventLivingUpdate;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(name = "AirStuck", type = TypeList.Movement)
public class AirStuck extends Module {
    private boolean oldIsFlying;
    private float yaw;
    private float pitch;
    private float yawoff;

    @Override
    public boolean onEvent(Event event) {
        if (mc.player == null) {
            return false;
        }

        if (event instanceof EventMotion e) {
            if (mc.player.ticksExisted % 10 == 0) {
                mc.player.connection.sendPacket(new CPlayerPacket(mc.player.isOnGround()));
            }

            e.setCancel(true);

            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }

            mc.player.rotationYawHead = this.yaw;
            mc.player.renderYawOffset = this.yawoff;
            mc.player.rotationPitchHead = this.pitch;
        }

        if (event instanceof EventLivingUpdate) {
            mc.player.noClip = true;
            mc.player.setOnGround(false);
            mc.player.setMotion(0.0, 0.0, 0.0);
            mc.player.abilities.isFlying = true;
        }

        if (event instanceof EventPacket e) {
            IPacket<?> packet = e.getPacket();

            if (packet instanceof CPlayerPacket) {
                CPlayerPacket cPlayerPacket = (CPlayerPacket) packet;
                if (cPlayerPacket.moving) {
                    cPlayerPacket.x = mc.player.getPosX();
                    cPlayerPacket.y = mc.player.getPosY();
                    cPlayerPacket.z = mc.player.getPosZ();
                }

                cPlayerPacket.onGround = mc.player.isOnGround();
                if (cPlayerPacket.rotating) {
                    cPlayerPacket.yaw = mc.player.rotationYaw;
                    cPlayerPacket.pitch = mc.player.rotationPitch;
                }
            }

            if (packet instanceof SJoinGamePacket) {
                this.toggle();
            }
        }

        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            this.oldIsFlying = mc.player.abilities.isFlying;
            mc.player.movementInput = new MovementInput();
            mc.player.moveForward = 0.0F;
            mc.player.moveStrafing = 0.0F;
            this.yaw = mc.player.rotationYaw;
            this.pitch = mc.player.rotationPitch;
            this.yawoff = mc.player.renderYawOffset;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.movementInput = new MovementInputFromOptions(mc.gameSettings);
            mc.player.abilities.isFlying = this.oldIsFlying;
        }
    }
}
