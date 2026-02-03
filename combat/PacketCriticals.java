package nuclear.module.impl.combat;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import mods.viaversion.vialoadingbase.ViaLoadingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.math.MathUtil;

import static nuclear.module.TypeList.Combat;

@Annotation(name = "PacketCriticals", type = Combat, desc = "Всегда даёт критический удар")
public class PacketCriticals extends Module {

    public final ModeSetting mode = new ModeSetting("Режим", "ReallyWorld", "ReallyWorld", "Grim 1.17+");
    public static boolean cancelCrit;

    public PacketCriticals() {
        addSettings(mode);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate && !this.allowedBypass() && mode.is("Grim 1.17+")) {
            ClientUtils.sendMessage(TextFormatting.GRAY + "Зайдите с " + TextFormatting.RED + "1.17+" + TextFormatting.GRAY + " для активаций " + TextFormatting.RED + "Criticals" + TextFormatting.GRAY + " или включите " + TextFormatting.RED + "Режим ReallyWorld");
            this.toggle();
        }
        if (event instanceof EventPacket e) {
            onPacket(e);
        }
        return false;
    }

    public void onPacket(EventPacket e) {
        if (e.isSendPacket() && e.getPacket() instanceof CUseEntityPacket packet) {
            if (packet.getAction() == CUseEntityPacket.Action.ATTACK) {
                Entity entity = packet.getEntityFromWorld(mc.world);
                if (entity == null || entity instanceof EnderCrystalEntity || cancelCrit) {
                    return;
                }
                sendGrimCrit();
            }
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

    public void sendGrimCrit() {
        if (Manager.FUNCTION_MANAGER.auraFunction.target == null) return;
        if (mode.is("ReallyWorld")) {
            if (mc.player.isOnGround()) return;
            double y = mc.player.getPosY();
            if (y == (int) y) return;
            if (mc.player.isInWeb() || mc.player.isInLava()) {
                mc.player.fallDistance = 0.001f;
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(mc.player.getPosX(), mc.player.getPosY() + (-(mc.player.fallDistance = MathUtil.randomizeFloat(1e-7F, 1e-6F))), mc.player.getPosZ(), AttackAura.rotate.x, AttackAura.rotate.y, false));
            }
        } else {
            if (mc.player.isOnGround()) return;
            double y = mc.player.getPosY();
            if (y == (int) y) return;
            mc.player.fallDistance = 0.001f;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(mc.player.getPosX(), mc.player.getPosY() + (-(mc.player.fallDistance = MathUtil.randomizeFloat(1e-7F, 1e-6F))), mc.player.getPosZ(), AttackAura.rotate.x, AttackAura.rotate.y, false));
        }
    }
}