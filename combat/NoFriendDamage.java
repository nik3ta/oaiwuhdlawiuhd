package nuclear.module.impl.combat;

import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CUseEntityPacket;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;


@Annotation(name = "NoFriendDamage", type = TypeList.Combat, desc = "Отключает урон по друзьям")
public class NoFriendDamage extends Module {

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket packet) {
            if (packet.getPacket() instanceof CUseEntityPacket useEntityPacket) {
                Entity entity = useEntityPacket.getEntityFromWorld(mc.world);
                if (entity instanceof RemoteClientPlayerEntity && Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()) && useEntityPacket.getAction() == CUseEntityPacket.Action.ATTACK) {
                    event.setCancel(true);
                }
            }
        }
        return false;
    }
}
