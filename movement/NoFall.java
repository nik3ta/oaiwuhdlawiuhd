package nuclear.module.impl.movement;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import mods.viaversion.vialoadingbase.ViaLoadingBase;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.utils.ClientUtils;
import nuclear.utils.math.ViaUtil;

@Annotation(name = "NoFall", type = TypeList.Movement, desc = "Убирает урон от падение")
public class NoFall extends Module {

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventUpdate && !this.allowedBypass()) {
            ClientUtils.sendMessage("Зайдите с " + TextFormatting.RED + "1.17+" + TextFormatting.WHITE + " для активаций " + TextFormatting.RED + "NoFall");
            this.toggle();
        }
        if (event instanceof EventUpdate) {
            onUpdate((EventUpdate) event);
        }
        return false;
    }

    public void onUpdate(EventUpdate e) {
        if (ViaUtil.allowedBypass() && mc.player.fallDistance > 2.5) {
            ViaUtil.sendPositionPacket(mc.player.getPosX(), mc.player.getPosY() + 1e-6, mc.player.getPosZ(), mc.player.rotationYaw, mc.player.rotationPitch, false);
            mc.player.fallDistance = 0;
        }
    }

    public boolean allowedBypass() {
        if (!ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
            return false;
        }
        for (UserConnection conn : Via.getManager().getConnectionManager().getConnections()) {
            if (conn == null) {
                return false;
            }
            if (conn.getProtocolInfo().getUsername().equalsIgnoreCase(mc.session.getProfile().getName())) {
                return true;
            }
        }
        return false;
    }
}