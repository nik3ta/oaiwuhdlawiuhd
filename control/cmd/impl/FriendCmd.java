package nuclear.control.cmd.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.cmd.Cmd;
import nuclear.control.cmd.CmdInfo;
import nuclear.control.friend.Friend;
import nuclear.control.friend.FriendManager;
import nuclear.control.Manager;
import nuclear.utils.language.Translated;

import java.util.ArrayList;
import java.util.List;


@CmdInfo(name = "friend", description = "Позволяет добавить человека в список друзей")
public class FriendCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        if (args.length > 1) {
            switch (args[1]) {
                case "add" -> {
                    final String friendName = args[2];
                    addFriend(friendName);
                }
                case "remove" -> {
                    final String friendName = args[2];
                    removeFriend(friendName);
                }
                case "list" -> friendList();
                case "clear" -> clearFriendList();
            }
        } else {
            error();
        }
    }

    /**
     * Функция добавления друзей в список
     *
     * @param friendName Имя друзей
     */
    private void addFriend(final String friendName) {
        final FriendManager friendManager = Manager.FRIEND_MANAGER;

        if (friendName.contains(Minecraft.getInstance().player.getName().getString())) {
            sendMessage(Translated.isRussian() ? "Unfortunately you can't add yourself as a friend :(" : "К сожалению вы не можете добавить самого себя в друзья :(");
            return;
        }
        if (friendManager.getFriends().stream().map(Friend::getName).toList().contains(friendName)) {
            sendMessage(Translated.isRussian() ? friendName + " already on your friends list" : friendName + " уже есть в списке друзей");
            return;
        }

        sendMessage(Translated.isRussian() ? friendName + " successfully added to your friends list!" : friendName + " успешно добавлен в список друзей!");
        friendManager.addFriend(friendName);
    }

    /**
     * Функция удаления друзей из списка друзей
     *
     * @param friendName Имя друзей
     */
    private void removeFriend(final String friendName) {
        final FriendManager friendManager = Manager.FRIEND_MANAGER;

        if (friendManager.isFriend(friendName)) {
            friendManager.removeFriend(friendName);
            sendMessage(Translated.isRussian() ? friendName + " has been removed from your friends list" : friendName + " был удален из списка друзей");
            return;
        }
        sendMessage(Translated.isRussian() ? friendName + " not on the friends list" : friendName + " нету в списке друзей");
    }

    /**
     * Функция вывода листа всех друзей
     */
    private void friendList() {
        final FriendManager friendManager = Manager.FRIEND_MANAGER;

        if (friendManager.getFriends().isEmpty()) {
            sendMessage(Translated.isRussian() ? "Friends list is empty" : "Список друзей пуст");
            return;
        }

        sendMessage(Translated.isRussian() ? "Friends list:" : "Список друзей:");
        for (Friend friend : friendManager.getFriends()) {
            sendMessage(TextFormatting.GRAY + friend.getName());
        }
    }

    /**
     * Функция очищы друзей
     */
    private void clearFriendList() {
        final FriendManager friendManager = Manager.FRIEND_MANAGER;

        if (friendManager.getFriends().isEmpty()) {
            sendMessage(Translated.isRussian() ? "Friends list is empty" : "Список друзей пуст");
            return;
        }

        friendManager.clearFriend();
        sendMessage(Translated.isRussian() ? "The friends list has been successfully cleared" : "Список друзей успешно очищен");
    }

    @Override
    public void error() {
        sendMessage(Translated.isRussian() ? TextFormatting.GRAY + "Error in use" + TextFormatting.WHITE + ":" : TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + "." + "friend add " + TextFormatting.GRAY + "<"
                + TextFormatting.RED + "name" + TextFormatting.GRAY + ">");
        sendMessage(TextFormatting.WHITE + "." + "friend remove " + TextFormatting.GRAY + "<"
                + TextFormatting.RED + "name" + TextFormatting.GRAY + ">");
        sendMessage(TextFormatting.WHITE + "." + "friend list" + TextFormatting.GRAY + (Translated.isRussian() ? " - shows a list of all friends" : " - показывает список всех друзей"));
        sendMessage(TextFormatting.WHITE + "." + "friend clear" + TextFormatting.GRAY + (Translated.isRussian() ? " - clears all friends" : " - очищает всех друзей"));
    }

    @Override
    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2 || (args.length > 2 && args[1].isEmpty())) {
            // Предлагаем подкоманды
            completions.add("add");
            completions.add("remove");
            completions.add("list");
            completions.add("clear");
        } else if (args.length >= 2) {
            String subCommand = args[1].toLowerCase();
            if (args.length == 3 || (args.length > 3 && args[2].isEmpty())) {
                // Предлагаем имена друзей для add и remove
                if (subCommand.equals("add")) {
                    // Можно предложить имена игроков онлайн, но пока оставим пустым
                    // или можно предложить имена из списка друзей для автодополнения
                } else if (subCommand.equals("remove")) {
                    // Предлагаем имена друзей для удаления
                    FriendManager friendManager = Manager.FRIEND_MANAGER;
                    for (Friend friend : friendManager.getFriends()) {
                        completions.add(friend.getName());
                    }
                }
            } else if (args.length > 3 && subCommand.equals("remove")) {
                // Фильтруем друзей по введенному тексту
                String partial = args[2].toLowerCase();
                FriendManager friendManager = Manager.FRIEND_MANAGER;
                for (Friend friend : friendManager.getFriends()) {
                    if (friend.getName().toLowerCase().startsWith(partial)) {
                        completions.add(friend.getName());
                    }
                }
            } else if (args.length > 3 && subCommand.equals("add")) {
                // Фильтруем по введенному тексту (можно добавить предложение имен игроков онлайн)
                String partial = args[2].toLowerCase();
                // Можно добавить логику для предложения имен игроков онлайн
            }
        }
        return completions;
    }
}
