package nuclear.control.cmd;

import nuclear.utils.ClientUtils;
import nuclear.utils.IMinecraft;
import nuclear.utils.language.Translated;

import java.util.ArrayList;
import java.util.List;

public abstract class Cmd implements IMinecraft {
    public final String command;
    public final String description;

    public Cmd() {
        command = this.getClass().getAnnotation(CmdInfo.class).name();
        description = this.getClass().getAnnotation(CmdInfo.class).description();
    }

    public abstract void run(String[] args) throws Exception;
    public abstract void error();

    public void sendMessage(String message) {
        ClientUtils.sendMessage(message);
    }

    /**
     * Возвращает список подсказок для tab completion
     * @param args Текущие аргументы команды
     * @return Список подсказок или null/пустой список, если подсказок нет
     */
    public List<String> tabComplete(String[] args) {
        return new ArrayList<>();
    }
}
