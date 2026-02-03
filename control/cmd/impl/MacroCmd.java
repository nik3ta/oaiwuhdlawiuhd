package nuclear.control.cmd.impl;

import net.minecraft.util.text.TextFormatting;
import nuclear.control.cmd.Cmd;
import nuclear.control.cmd.CmdInfo;
import nuclear.control.Manager;
import nuclear.utils.math.KeyMappings;

@CmdInfo(name = "macro", description = "Даёт возможность отправлять команду при нажатии кнопки")
public class MacroCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        if (args.length < 2) {
            error();
            return;
        }

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 4) {
                    sendMessage(TextFormatting.RED + "Недостаточно аргументов для добавления макроса");
                    return;
                }
                String buttonName = args[2].toUpperCase();
                Integer keycode = KeyMappings.keyMap.get(buttonName);

                if (keycode == null) {
                    sendMessage(TextFormatting.RED + "Не найдена кнопка с названием " + buttonName);
                    sendMessage(TextFormatting.GRAY + "Доступные кнопки: " + KeyMappings.keyMap.keySet());
                    return;
                }

                if (Manager.MACRO_MANAGER.hasMacro(keycode)) {
                    sendMessage(TextFormatting.RED + "Макрос для кнопки " + buttonName + " уже существует");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                String message = sb.toString().trim();
                Manager.MACRO_MANAGER.addMacro(message, keycode);
                sendMessage(TextFormatting.GREEN + "Добавлен макрос для кнопки " + TextFormatting.RED + "\"" + buttonName + "\"" +
                        TextFormatting.WHITE + " с командой " + TextFormatting.RED + "\"" + message + "\"");
            }
            case "clear" -> {
                if (Manager.MACRO_MANAGER.isEmpty()) {
                    sendMessage(TextFormatting.RED + "Список макросов пуст");
                } else {
                    Manager.MACRO_MANAGER.clearList();
                    sendMessage(TextFormatting.GREEN + "Список макросов успешно очищен");
                }
            }
            case "remove" -> {
                if (args.length < 3) {
                    sendMessage(TextFormatting.RED + "Укажите кнопку макроса для удаления");
                    return;
                }
                String buttonName = args[2].toUpperCase();
                Integer keycode = KeyMappings.keyMap.get(buttonName);
                if (keycode == null || !Manager.MACRO_MANAGER.hasMacro(keycode)) {
                    sendMessage(TextFormatting.RED + "Макрос для кнопки " + buttonName + " не найден");
                    return;
                }
                Manager.MACRO_MANAGER.deleteMacro(keycode);
                sendMessage(TextFormatting.GREEN + "Макрос для кнопки " + TextFormatting.RED + "\"" + buttonName + "\"" +
                        TextFormatting.WHITE + " был удален");
            }
            case "list" -> {
                if (Manager.MACRO_MANAGER.isEmpty()) {
                    sendMessage(TextFormatting.RED + "Список макросов пуст");
                } else {
                    sendMessage(TextFormatting.GREEN + "Список макросов:");
                    Manager.MACRO_MANAGER.getMacros().forEach(macro ->
                            sendMessage(TextFormatting.WHITE + "Команда: " + TextFormatting.RED + macro.getMessage() +
                                    TextFormatting.WHITE + ", Кнопка: " + TextFormatting.RED + macro.getKey()));
                }
            }
            default -> error();
        }
    }

    @Override
    public void error() {
        sendMessage(TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + "." + "macro add " + TextFormatting.GRAY + "<" +
                TextFormatting.RED + "key" + TextFormatting.GRAY + "> <" +
                TextFormatting.RED + "message" + TextFormatting.GRAY + ">");
        sendMessage(TextFormatting.WHITE + "." + "macro remove " + TextFormatting.GRAY + "<" +
                TextFormatting.RED + "key" + TextFormatting.GRAY + ">");
        sendMessage(TextFormatting.WHITE + "." + "macro list");
        sendMessage(TextFormatting.WHITE + "." + "macro clear");
    }
}