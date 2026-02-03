package nuclear.control.cmd.impl;

import net.minecraft.util.text.TextFormatting;
import nuclear.control.cmd.Cmd;
import nuclear.module.api.Module;
import nuclear.utils.language.Translated;
import org.lwjgl.glfw.GLFW;
import nuclear.control.cmd.CmdInfo;
import nuclear.control.Manager;
import nuclear.utils.math.KeyMappings;

import java.util.ArrayList;
import java.util.List;


@CmdInfo(name = "bind", description = "Позволяет привязать модуль к определенной клавише")
public class BindCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        try {
            if (args.length >= 2) {
                switch (args[1].toLowerCase()) {
                    case "list" -> listBoundKeys();
                    case "clear" -> clearAllBindings();
                    case "add" -> {
                        if (args.length >= 4) {
                            addKeyBinding(args[2], args[3]);
                        } else {
                            error();
                        }
                    }
                    case "remove" -> {
                        if (args.length >= 2) {
                            removeKeyBinding(args[2]);
                        } else {
                            error();
                        }
                    }
                    default -> error();
                }
            } else {
                error();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод для вывода списка модулей с привязанными клавишами
     */
    private void listBoundKeys() {
        sendMessage(Translated.isRussian() ? TextFormatting.GRAY + "List of all modules with key bindings:" : TextFormatting.GRAY + "Список всех модулей с привязанными клавишами:");
        for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
            if (f.bind == 0) continue;
            sendMessage(f.name + " [" + TextFormatting.GRAY + (GLFW.glfwGetKeyName(f.bind, -1) == null ? "" : GLFW.glfwGetKeyName(f.bind, -1)) + TextFormatting.RESET + "]");
        }
    }

    /**
     * Метод для очистки всех привязок модулей
     */
    private void clearAllBindings() {
        for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
            f.bind = 0;
        }
        sendMessage(Translated.isRussian() ? TextFormatting.GREEN + "All keys have been unlinked from modules" : TextFormatting.GREEN + "Все клавиши были отвязаны от модулей");
    }

    /**
     * Метод для привязывания заданной клавиши к модулю
     *
     * @param moduleName Имя модуля
     * @param keyName    Название клавиши
     */
    private void addKeyBinding(String moduleName, String keyName) {
        Integer key = null;

        try {
            key = KeyMappings.keyMap.get(keyName.toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Module module = Manager.FUNCTION_MANAGER.get(moduleName);
        if (key != null) {
            if (module != null) {
                module.bind = key;
                sendMessage(Translated.isRussian() ? "Key" + TextFormatting.GRAY + keyName + TextFormatting.WHITE + " was bound to the module " + TextFormatting.RED + module.name : "Клавиша" + TextFormatting.GRAY + keyName + TextFormatting.WHITE + " была привязана к модулю " + TextFormatting.RED + module.name);
            } else {
                sendMessage(Translated.isRussian() ? "Module " + moduleName + " was not found" : "Модуль " + moduleName + " не был найден");
            }
        } else {
            sendMessage(Translated.isRussian() ? "Key " + keyName + " was not found!" : "Клавиша " + keyName + " не была найдена!");
        }
    }

    /**
     * Метод для удаления привязки клавиши
     *
     * @param moduleName Имя модуля
     */
    private void removeKeyBinding(String moduleName) {
        for (Module f : Manager.FUNCTION_MANAGER.getFunctions()) {
            if (f.name.equalsIgnoreCase(moduleName)) {
                f.bind = 0;
                sendMessage(Translated.isRussian() ? "Key " + TextFormatting.RESET + " was unlinked from the module " + TextFormatting.GRAY + f.name : "Клавиша " + TextFormatting.RESET + " была отвязана от модуля " + TextFormatting.GRAY + f.name);
            }
        }
    }

    /**
     * Метод для вывода ошибки неверного исполнения команды
     */
    @Override
    public void error() {
        sendMessage(Translated.isRussian() ? TextFormatting.WHITE + "Invalid command syntax. " + TextFormatting.GRAY + "Use:" : TextFormatting.WHITE + "Неверный синтаксис команды. " + TextFormatting.GRAY + "Используйте:");
        sendMessage(TextFormatting.WHITE + ".bind add " + TextFormatting.DARK_GRAY + "<name> <key>");
        sendMessage(TextFormatting.WHITE + ".bind remove " + TextFormatting.DARK_GRAY + "<name>");
        sendMessage(TextFormatting.WHITE + ".bind list");
        sendMessage(TextFormatting.WHITE + ".bind clear");
    }

    @Override
    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            completions.add("add");
            completions.add("remove");
            completions.add("list");
            completions.add("clear");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
            // Предлагаем имена модулей
            for (nuclear.module.api.Module module : Manager.FUNCTION_MANAGER.getFunctions()) {
                completions.add(module.name);
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
            // Предлагаем клавиши
            completions.addAll(nuclear.utils.math.KeyMappings.keyMap.keySet());
        } else if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
            // Предлагаем имена модулей с привязками
            for (nuclear.module.api.Module module : Manager.FUNCTION_MANAGER.getFunctions()) {
                if (module.bind != 0) {
                    completions.add(module.name);
                }
            }
        }
        return completions;
    }
}