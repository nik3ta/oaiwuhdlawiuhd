package nuclear.module.impl.other;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BindSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.misc.TimerUtil;

@Annotation(name = "ClickFriend", type = TypeList.Other, desc = "Добавляет друга при нажатие на клавишу")
public class ClickFriend extends Module {
    private final TimerUtil timerUtil = new TimerUtil();

    private BindSetting clickKey = new BindSetting("Кнопка", -98);

    public ClickFriend() {
        addSettings(clickKey);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey e) {
            if (e.key == clickKey.getKey()) {
                handleKeyPressEvent();
            }
        }
        return false;
    }

    private void handleKeyPressEvent() {
        if (timerUtil.hasTimeElapsed(50L) && mc.pointedEntity instanceof PlayerEntity && Manager.FRIEND_MANAGER != null) {
            String entityName = mc.pointedEntity.getName().getString();
            if (Manager.FRIEND_MANAGER.isFriend(entityName)) {
                Manager.FRIEND_MANAGER.removeFriend(entityName);
                displayRemoveFriendMessage(entityName);
            } else {
                Manager.FRIEND_MANAGER.addFriend(entityName);
                displayAddFriendMessage(entityName);
            }
            timerUtil.reset();
        }
    }

    private void displayRemoveFriendMessage(String friendName) {
        ClientUtils.sendMessage(TextFormatting.RESET + "Удалил " + TextFormatting.RED + friendName + TextFormatting.RESET + " из друзей");
    }

    private void displayAddFriendMessage(String friendName) {
        ClientUtils.sendMessage(TextFormatting.RESET + "Добавил " + TextFormatting.GREEN + friendName + TextFormatting.RESET + " в друзья");
    }
}
