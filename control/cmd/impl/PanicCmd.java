package nuclear.control.cmd.impl;

import nuclear.control.cmd.Cmd;
import nuclear.control.cmd.CmdInfo;
import nuclear.control.Manager;
import nuclear.utils.ClientUtils;
import nuclear.utils.language.Translated;

@CmdInfo(name = "panic", description = "Выключает все функции чита")

public class PanicCmd extends Cmd {
    @Override
    public void run(String[] args) throws Exception {
        if (args.length == 1) {
            Manager.FUNCTION_MANAGER.getFunctions().stream().filter(function -> function.state).forEach(function -> function.setState(false));
            ClientUtils.sendMessage(Translated.isRussian() ? "Turned off all modules!" : "Выключены все модули!");
        } else error();
    }

    @Override
    public void error() {

    }
}
