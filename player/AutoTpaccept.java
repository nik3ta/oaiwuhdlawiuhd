package nuclear.module.impl.player;

import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.friend.Friend;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

import java.util.Arrays;


@Annotation(name = "AutoTpaccept", type = TypeList.Player)
public class AutoTpaccept extends Module {
    private final BooleanSetting onlyfriends = new BooleanSetting("Только друзья",
            "Only friends", false);

    private final String[] teleportMessages = new String[]{"has requested teleport", "просит телепортироваться", "хочет к вам телепортироваться", "хочет к вам телепортироваться", "хочет телепортироваться"};

    public AutoTpaccept() {
        addSettings(onlyfriends);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket packetEvent) {
            if (packetEvent.isReceivePacket()) {
                if (packetEvent.getPacket() instanceof SChatPacket packetChat) {
                    handleReceivePacket(packetChat);
                }
            }
        }
        return false;
    }

    /**
     * Обрабатывает получение пакет чата.
     *
     * @param packet Пакет чата
     */
    private void handleReceivePacket(SChatPacket packet) {
        String message = TextFormatting.getTextWithoutFormattingCodes(packet.getChatComponent().getString());

        if (isTeleportMessage(message)) {
            if (onlyFriendsEnabled()) {
                handleTeleportWithFriends(message);
                return;
            }
            acceptTeleport();

        }
    }

    /**
     * Проверяет, является ли сообщение запросом телепортации.
     *
     * @param message Сообщение чата
     * @return true, если сообщение является запросом телепортации, иначе false
     */
    private boolean isTeleportMessage(String message) {
        return Arrays.stream(this.teleportMessages)
                .map(String::toLowerCase)
                .anyMatch(message::contains);
    }

    /**
     * Проверяет, включен ли режим "только для друзей".
     *
     * @return true, если режим "только для друзей" включен, иначе false
     */
    private boolean onlyFriendsEnabled() {
        return onlyfriends.get();
    }

    /**
     * Обрабатывает запрос телепортации, когда включена опция "только для друзей".
     *
     * @param message Сообщение чата
     */
    private void handleTeleportWithFriends(String message) {
        for (Friend friend : Manager.FRIEND_MANAGER.getFriends()) {

            StringBuilder builder = new StringBuilder();
            char[] buffer = message.toCharArray();
            for (int w = 0; w < buffer.length; w++) {
                char c = buffer[w];
                if (c == '§') {
                    w++;
                } else {
                    builder.append(c);
                }
            }

            if (builder.toString().contains(friend.getName()))
                acceptTeleport();
        }
    }

    /**
     * Отправляет команду для принятия телепортации.
     */
    private void acceptTeleport() {
        mc.player.sendChatMessage("/tpaccept");
    }
}
