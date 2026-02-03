package nuclear.utils.math;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.experimental.UtilityClass;
import mods.viaversion.vialoadingbase.ViaLoadingBase;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import nuclear.utils.IMinecraft;

@UtilityClass
public class ViaUtil implements IMinecraft {
    public boolean allowedBypass() {
        if (!ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) return false;
        for (UserConnection conn : Via.getManager().getConnectionManager().getConnections()) {
            if (conn == null) return false;
            if (conn.getProtocolInfo().getUsername().equalsIgnoreCase(mc.session.getProfile().getName())) return true;
        }
        return false;
    }

    public void sendPositionPacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y, z, yaw, pitch, onGround));
    }

    public void sendPositionPacket(float yaw, float pitch, boolean sendUseItemPacket) {
        mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ(), yaw, pitch, mc.player.isOnGround()));
        if (sendUseItemPacket) sendUseItemPacket(true);
    }

    public void sendPositionPacket() {
        mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ(), mc.player.rotationYaw, mc.player.rotationPitch, mc.player.isOnGround()));
    }

    public void sendUseItemPacket(boolean checkHandActive) {
        if (checkHandActive && mc.player.isHandActive()) return;
        mc.player.connection.sendPacketWithoutEvent(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
    }
}
