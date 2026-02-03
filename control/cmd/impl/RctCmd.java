package nuclear.control.cmd.impl;

import net.minecraft.scoreboard.ScoreObjective;
import nuclear.control.cmd.Cmd;
import nuclear.control.cmd.CmdInfo;

@CmdInfo(name = "rct", description = "Реконектится на анахрий FunTime")
public class RctCmd extends Cmd {

    @Override
    public void run(String[] args) throws Exception {
        String anca = "";
        for (ScoreObjective team : mc.world.getScoreboard().getScoreObjectives()) {
            String an = team.getDisplayName().getString();
            if (an.contains("Анархия-")) {
                anca = an.split("Анархия-")[1];
                mc.player.sendChatMessage("/hub");
                break;
            }
        }
        String finalAnca = anca;
        new Thread(() -> {
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mc.player.sendChatMessage("/an" + finalAnca);
        }).start();

    }

    @Override
    public void error() {
    }

}