package nuclear.module.impl.player;

import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(name = "ItemSwapFix", type = TypeList.Player)
public class ItemSwapFix extends Module {

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket packetEvent) {
            if (packetEvent.isReceivePacket()) {
                if (packetEvent.getPacket() instanceof SHeldItemChangePacket packetHeldItemChange) {
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                    event.setCancel(true);
                }
            }
        }
        return false;
    }
}
