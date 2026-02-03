package nuclear.control.cmd.impl;

import net.minecraft.util.text.TextFormatting;
import nuclear.control.cmd.Cmd;
import nuclear.control.cmd.CmdInfo;
import nuclear.control.Manager;
import nuclear.utils.ClientUtils;

@CmdInfo(name = "help", description = "Список команд чита")
public class HelpCmd extends Cmd {
    @Override
    public void run(String[] args) throws Exception {
        for (Cmd cmd : Manager.COMMAND_MANAGER.getCommands()) {
            if (cmd instanceof HelpCmd) continue;
            ClientUtils.sendMessage(TextFormatting.WHITE + cmd.description + TextFormatting.GRAY +  " " + TextFormatting.RED + cmd.command);
        }
    }

    @Override
    public void error() {

    }
}
