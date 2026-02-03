package nuclear.module.impl.other;

import net.minecraft.block.*;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;

@Annotation(name = "Optimization", type = TypeList.Other, desc = "Удаляет всякое что бы оптимизировать вашу игру")
public class Optimization extends Module {
    public final MultiBoxSetting optimizeSelection = new MultiBoxSetting("Оптимизировать", new BooleanSetting("Растения", true), new BooleanSetting("Партиклы", true), new BooleanSetting("Облака", true), new BooleanSetting("Графику неба", true), new BooleanSetting("Энтити", true));

    public Optimization() {
        addSettings(optimizeSelection);
    }

    @Override
    public boolean onEvent(Event event) {
        if (optimizeSelection.get("Облака")) {
            mc.gameSettings.ofSky = false;
        }
        if (optimizeSelection.get("Графику неба")) {
            mc.gameSettings.ofCustomSky = false;
        }
        if (optimizeSelection.get("Энтити")) {
            mc.gameSettings.entityShadows = false;
        }
        return false;
    }

    public boolean canRender(Block block) {
        return (!(block instanceof TallGrassBlock)
                && !(block instanceof FlowerBlock)
                && !(block instanceof DoublePlantBlock)
                && !(block instanceof DeadBushBlock)
                && !(block instanceof MushroomBlock)
                && !(block instanceof CropsBlock)
                && !(block instanceof NetherSproutsBlock)) || (!optimizeSelection.get(0) || !Manager.FUNCTION_MANAGER.optimization.state);
    }

    public void onDisable() {
        super.onDisable();
        mc.gameSettings.ofSky = true;
        mc.gameSettings.ofCustomSky = true;
        mc.gameSettings.entityShadows = true;
    }
}