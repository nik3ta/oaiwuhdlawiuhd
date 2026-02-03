package nuclear.module.impl.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CChatMessagePacket;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.ClientUtils;

import java.awt.*;

@Annotation(name = "AutoLeave", type = TypeList.Player)
public class AutoLeave extends Module {

    public SliderSetting range = new SliderSetting("Расстояние", 15, 5, 40, 1);
    public ModeSetting mode = new ModeSetting("Как выйти?", "/spawn", "/spawn", "/hub", "kick");
    public BooleanSetting health = new BooleanSetting("По здоровью", false);
    public SliderSetting healthSlider = new SliderSetting("Здоровье", 10, 5, 20, 1).setVisible(() -> health.get());

    public AutoLeave() {
        addSettings(range, mode, health, healthSlider);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventMotion e) {
            if (health.get()) {
                if (mc.player.getHealth() <= healthSlider.getValue().floatValue()) {
                    if (mode.is("kick")) {
                        mc.player.connection.getNetworkManager().closeChannel(ClientUtils.gradient("Вы вышли с сервера! \n" +" Вижу вас!", new Color(121, 208, 255).getRGB(), new Color(96, 133, 255).getRGB()));
                    } else {
                        mc.player.connection.sendPacket(new CChatMessagePacket(mode.get()));
                    }
                }
                setState(false);
                return false;
            }

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (player.isBot) continue;
                if (Manager.FRIEND_MANAGER.isFriend(player.getGameProfile().getName())) {
                    continue;
                }

                if (mc.player.getDistance(player) <= range.getValue().floatValue()) {
                    if (mode.is("kick")) {
                        mc.player.connection.getNetworkManager().closeChannel(ClientUtils.gradient("Вы вышли с сервера! \n" + player.getGameProfile().getName(), new Color(121, 208, 255).getRGB(), new Color(96, 133, 255).getRGB()));
                    } else {
                        mc.player.connection.sendPacket(new CChatMessagePacket(mode.get()));
                    }
                    setState(false);
                    break;
                }
            }
        }
        return false;
    }
}
