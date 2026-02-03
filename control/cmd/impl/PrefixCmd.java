package nuclear.control.cmd.impl;

import nuclear.control.cmd.Cmd;
import nuclear.control.cmd.CmdInfo;
import nuclear.control.Manager;
import nuclear.utils.ClientUtils;
import net.minecraft.util.text.TextFormatting;

@CmdInfo(name = "prefix", description = "Установить префикс для команд")
public class PrefixCmd extends Cmd {
    @Override
    public void run(String[] args) {
        if (args.length != 2) {
            ClientUtils.sendMessage(TextFormatting.RED + "Использование: .prefix <новый_префикс>");
            return;
        }

        String newPrefix = args[1];
        if (!isValidPrefix(newPrefix)) {
            ClientUtils.sendMessage(TextFormatting.RED + "Недопустимый символ в префиксе!");
            return;
        }

        Manager.COMMAND_MANAGER.setPrefix(newPrefix);
        ClientUtils.sendMessage(TextFormatting.GREEN + "Префикс установлен на: " + newPrefix);
    }

    private boolean isValidPrefix(String prefix) {
        return prefix.length() == 1;
    }

    @Override
    public void error() {
    }
}


