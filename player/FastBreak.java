package nuclear.module.impl.player;

import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "FastBreak", type = TypeList.Player, desc = "Ускоряет ломание блоков")
public class FastBreak extends Module {
    public SliderSetting speed = new SliderSetting("Ускорение", 0.7f, 0.3f, 1.0f, 0.1f);

    public FastBreak() {
        addSettings(speed);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            mc.playerController.blockHitDelay = 0;
            if (mc.playerController.getIsHittingBlock()) {
                float speedMultiplier = speed.getValue().floatValue();
                mc.playerController.curBlockDamageMP += speedMultiplier * 0.2f;
                if (mc.playerController.curBlockDamageMP >= 1.0f) {
                    mc.playerController.curBlockDamageMP = 1.0f;
                }
            }
        }
        return false;
    }
}