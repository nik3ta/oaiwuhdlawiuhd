package nuclear.control.cmd.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import nuclear.control.cmd.Cmd;
import nuclear.control.cmd.CmdInfo;
import nuclear.utils.ClientUtils;
import nuclear.utils.language.Translated;

@CmdInfo(name = "hclip", description = "Телепортирует вас вперед")
public class HClipCmd extends Cmd {
    @Override
    public void run(String[] args) throws Exception {
        if (args.length < 2) {
            ClientUtils.sendMessage(Translated.isRussian() ? "Specify teleportation distance" : "Укажите дистанцию телепортации");
            return;
        }

        try {
            double distance = Double.parseDouble(args[1]);
            Vector3d tp = Minecraft.getInstance().player.getLook(1F).scale(distance);
            Minecraft.getInstance().player.setPosition(
                    Minecraft.getInstance().player.getPosX() + tp.x,
                    Minecraft.getInstance().player.getPosY(),
                    Minecraft.getInstance().player.getPosZ() + tp.z
            );
            ClientUtils.sendMessage(Translated.isRussian() ? "You are teleported to " + TextFormatting.RED + distance + TextFormatting.WHITE + " blocks forward" : "Вы телепортированы на " + TextFormatting.RED + distance + TextFormatting.WHITE + " блоков вперед");
        } catch (NumberFormatException e) {
            ClientUtils.sendMessage(Translated.isRussian() ? "Please enter a valid number for the distance" : "Введите корректное число для дистанции");
        }
    }

    @Override
    public void error() {
    }
}
