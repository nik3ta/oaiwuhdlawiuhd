package nuclear.control.cmd;

import nuclear.control.cmd.impl.*;
import nuclear.control.Manager;
import nuclear.utils.ClientUtils;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdManager {
    public List<Cmd> cmdList = new ArrayList<>();
    public boolean isMessage;
    private String prefix = ".";

    public void init() {
        cmdList.addAll(Arrays.asList(
                new TargetCmd(),
                new RctCmd(),
                new HClipCmd(),
                new VClipCmd(),
                new HelpCmd(),
                new MacroCmd(),
                new BindCmd(),
                new ConfigCmd(),
                new FriendCmd(),
                new PanicCmd(),
                new StaffCmd(),
                new PrefixCmd(),
                new GPSCmd()
        ));
    }

    public void setPrefix(String newPrefix) {
        this.prefix = newPrefix;
    }

    public void runCommands(String message) {
        if (Manager.FUNCTION_MANAGER.noCommands.state) {
            isMessage = false;
            return;
        }
        if (message.startsWith(prefix)) {
            for (Cmd cmd : Manager.COMMAND_MANAGER.getCommands()) {
                if (message.startsWith(prefix + cmd.command)) {
                    try {
                        cmd.run(message.split(" "));
                    } catch (Exception ex) {
                        cmd.error();
                        ex.printStackTrace();
                    }
                    isMessage = true;
                    return;
                }
            }
            ClientUtils.sendMessage(TextFormatting.RED + "Команда не найдена!");
            ClientUtils.sendMessage(TextFormatting.GRAY + "Используйте " + TextFormatting.RED + prefix + "help" + TextFormatting.GRAY + " для списка всех команд.");
            isMessage = true;
        } else {
            isMessage = false;
        }
    }

    public List<Cmd> getCommands() {
        return cmdList;
    }

    public List<String> tabComplete(String input) {
        List<String> completions = new ArrayList<>();
        
        if (!input.startsWith(prefix)) {
            return completions;
        }
        
        String commandPart = input.substring(prefix.length());
        String[] parts = commandPart.split(" ", -1);
        
        // Убираем пустые элементы в конце (если есть пробелы в конце)
        int actualLength = parts.length;
        while (actualLength > 0 && parts[actualLength - 1].isEmpty()) {
            actualLength--;
        }
        
        if (actualLength == 0 || parts[0].isEmpty()) {
            // Предлагаем все команды
            for (Cmd cmd : cmdList) {
                completions.add(prefix + cmd.command);
            }
        } else if (actualLength == 1) {
            // Проверяем, является ли это полной командой (с пробелом в конце)
            String commandName = parts[0].toLowerCase();
            boolean isCompleteCommand = input.endsWith(" ");
            
            if (isCompleteCommand) {
                // Команда введена полностью с пробелом, предлагаем аргументы
                for (Cmd cmd : cmdList) {
                    if (cmd.command.toLowerCase().equals(commandName)) {
                        List<String> cmdCompletions = cmd.tabComplete(parts);
                        if (cmdCompletions != null && !cmdCompletions.isEmpty()) {
                            completions.addAll(cmdCompletions);
                        }
                        break;
                    }
                }
            } else {
                // Предлагаем команды, начинающиеся с введенного текста
                String partial = commandName;
                for (Cmd cmd : cmdList) {
                    if (cmd.command.toLowerCase().startsWith(partial)) {
                        completions.add(prefix + cmd.command);
                    }
                }
            }
        } else {
            // Команда уже выбрана, ищем её и получаем подсказки для аргументов
            String commandName = parts[0].toLowerCase();
            for (Cmd cmd : cmdList) {
                if (cmd.command.toLowerCase().equals(commandName)) {
                    List<String> cmdCompletions = cmd.tabComplete(parts);
                    if (cmdCompletions != null && !cmdCompletions.isEmpty()) {
                        completions.addAll(cmdCompletions);
                    }
                    break;
                }
            }
        }
        
        return completions;
    }
}