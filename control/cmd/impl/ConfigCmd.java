package nuclear.control.cmd.impl;

import net.minecraft.util.text.TextFormatting;
import nuclear.control.cmd.Cmd;
import nuclear.control.cmd.CmdInfo;
import nuclear.control.config.ConfigManager;
import nuclear.control.Manager;
import nuclear.utils.language.Translated;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@CmdInfo(name = "cfg", description = "Через эту команду можно управлять конфигами")
public class ConfigCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        if (args.length > 1) {
            ConfigManager configManager = Manager.CONFIG_MANAGER;
            switch (args[1]) {
                case "save" -> {
                    String configName = args[2];
                    configManager.saveConfiguration(configName);
                    sendMessage(Translated.isRussian() ? "Configuration " + TextFormatting.GRAY + args[2] + TextFormatting.RESET + " successfully saved." : "Конфигурация " + TextFormatting.GRAY + args[2] + TextFormatting.RESET + " успешно сохранена.");
                }
                case "load" -> {
                    String configName = args[2];
                    configManager.loadConfiguration(configName, false);
                }
                case "remove" -> {
                    String configName = args[2];
                    try {
                        configManager.deleteConfig(configName);
                        sendMessage(Translated.isRussian() ? "Configuration " + TextFormatting.GRAY + args[2] + TextFormatting.RESET + " successfully deleted." : "Конфигурация " + TextFormatting.GRAY + args[2] + TextFormatting.RESET + " успешно удалена.");
                    } catch (Exception e) {
                        sendMessage(Translated.isRussian() ? "Failed to delete configuration named " + configName + " maybe it's just not there." : "Не удалось удалить конфигурацию с именем " + configName + " возможно её просто нет.");
                    }
                }
                case "list" -> {
                    if (configManager.getAllConfigurations().isEmpty()) {
                        sendMessage(Translated.isRussian() ? "The list of configs is empty." : "Список конфигов пуст.");
                        return;
                    }
                    for (String s : configManager.getAllConfigurations()) {
                        sendMessage(s.replace(".cfg", ""));
                    }
                }
                case "dir" -> {
                    try {
                        Runtime.getRuntime().exec("explorer " + ConfigManager.CONFIG_DIR.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            error();
        }
    }

    @Override
    public void error() {
        sendMessage(Translated.isRussian() ? TextFormatting.GRAY + "Error in use" + TextFormatting.WHITE + ":" : TextFormatting.GRAY + "Ошибка в использовании" + TextFormatting.WHITE + ":");
        sendMessage(TextFormatting.WHITE + "." + "cfg load " + TextFormatting.GRAY + "<name>");
        sendMessage(TextFormatting.WHITE + "." + "cfg save " + TextFormatting.GRAY + "<name>");
        sendMessage(TextFormatting.WHITE + "." + "cfg remove " + TextFormatting.GRAY + "<name>");
        sendMessage(TextFormatting.WHITE + "." + "cfg list" + TextFormatting.GRAY
                + (Translated.isRussian() ? " - show list of configs" : " - показать список конфигов"));
        sendMessage(TextFormatting.WHITE + "." + "cfg dir" + TextFormatting.GRAY
                + (Translated.isRussian() ? " - open the config folder" : " - открыть папку с конфигами"));
    }

    @Override
    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2 || (args.length > 2 && args[1].isEmpty())) {
            // Предлагаем подкоманды
            completions.add("load");
            completions.add("save");
            completions.add("remove");
            completions.add("list");
            completions.add("dir");
        } else if (args.length >= 2) {
            String subCommand = args[1].toLowerCase();
            if (args.length == 3 || (args.length > 3 && args[2].isEmpty())) {
                // Предлагаем имена конфигов для load, save, remove
                if (subCommand.equals("load") || subCommand.equals("save") || subCommand.equals("remove")) {
                    ConfigManager configManager = Manager.CONFIG_MANAGER;
                    for (String configName : configManager.getAllConfigurations()) {
                        completions.add(configName.replace(".cfg", ""));
                    }
                }
            } else if (args.length > 3 && (subCommand.equals("load") || subCommand.equals("save") || subCommand.equals("remove"))) {
                // Фильтруем конфиги по введенному тексту
                String partial = args[2].toLowerCase();
                ConfigManager configManager = Manager.CONFIG_MANAGER;
                for (String configName : configManager.getAllConfigurations()) {
                    String name = configName.replace(".cfg", "");
                    if (name.toLowerCase().startsWith(partial)) {
                        completions.add(name);
                    }
                }
            }
        }
        return completions;
    }
}
