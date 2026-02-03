package nuclear.module.impl.movement;

import net.minecraft.block.BlockState;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.player.EventBlockCollide;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.SliderSetting;

import java.util.ArrayList;
import java.util.List;

@Annotation(
        name = "NoClip",
        type = TypeList.Movement,
        desc = "Позволяет пройти через стены"
)
public class NoClip extends Module {
    private final List<IPacket<?>> bufferedPackets = new ArrayList<>();
    private final SliderSetting semiPackets = new SliderSetting("Кол-во попыток", 4f, 1f, 5f, 1f);
    public final BooleanSetting slow = new BooleanSetting("Замедлятся", true);
    private boolean semiPacketSent;
    private boolean skipReleaseOnDisable;

    public NoClip() {
        addSettings(semiPackets, slow);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket) {
            onPacket((EventPacket) event);
            return true;
        } else if (event instanceof EventBlockCollide) {
            BlockCollide((EventBlockCollide) event);
            return true;
        } else if (event instanceof EventUpdate) {
            eventUpdate((EventUpdate) event);
            return true;
        }
        return false;
    }

    public void onPacket(EventPacket eventPacket) {
        if (mc.player == null || mc.player.connection == null) return;

        IPacket<?> packet = eventPacket.getPacket();
        if (packet instanceof CPlayerPacket) {
            bufferedPackets.add(packet);
            eventPacket.cancel();
        }
    }

    public void BlockCollide(EventBlockCollide eventPacket) {
        if (mc.player == null || mc.player.connection == null) return;
        int playerBlockY = mc.player.getPosition().getY();
        if (eventPacket.getPos().getY() >= playerBlockY || mc.gameSettings.keyBindSneak.isKeyDown()) {
            eventPacket.setCancel(true);
        }
    }

    public void eventUpdate(EventUpdate eventPacket) {
        if (mc.player == null || mc.world == null) return;
        boolean noSolidInAABB = mc.world.getStatesInArea(mc.player.getBoundingBox().shrink(0.001D))
                .noneMatch(BlockState::isSolid);
        long totalStates = mc.world.getStatesInArea(mc.player.getBoundingBox().shrink(0.001D)).count();
        long solidStates = mc.world.getStatesInArea(mc.player.getBoundingBox().shrink(0.001D)).filter(BlockState::isSolid).count();
        boolean semiInsideBlock = solidStates > 0 && solidStates < totalStates;

        if (slow.get()) {
            mc.player.setMotion(mc.player.getMotion().x * 0.725, mc.player.getMotion().y, mc.player.getMotion().z * 0.725);
            mc.player.setVelocity(mc.player.getMotion().x, 0, mc.player.getMotion().z);
        }
        if (!semiPacketSent && semiInsideBlock) {
            int packetsToSend = Math.max(1, Math.min(10, semiPackets.getValue().intValue()));
            double x = mc.player.getPosX();
            double y = mc.player.getPosY();
            double z = mc.player.getPosZ();
            boolean onGround = mc.player.isOnGround();
            for (int i = 0; i < packetsToSend; i++) {
                mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, onGround));
            }
            semiPacketSent = true;
            return;
        }

        if (semiPacketSent && noSolidInAABB) {
            skipReleaseOnDisable = true;
            setState(false);
        }
    }

    @Override
    public void onDisable() {
        double x = mc.player.getPosX();
        double y = mc.player.getPosY();
        double z = mc.player.getPosZ();
        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y + 0.0625, z, false));
        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, false));
        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y + 0.03125, z, true));
        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, mc.player.isOnGround()));
        if (mc.player != null && mc.player.connection != null && !bufferedPackets.isEmpty()) {
            for (IPacket<?> packet : bufferedPackets) {
                mc.player.connection.sendPacketWithoutEvent(packet);
            }
            bufferedPackets.clear();
        }

        super.onDisable();
    }

    @Override
    public void onEnable() {
        bufferedPackets.clear();
        semiPacketSent = false;
        skipReleaseOnDisable = false;
        super.onEnable();
    }
}