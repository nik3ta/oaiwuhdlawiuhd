package nuclear.module.impl.movement;

import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "AntiTarget", type = TypeList.Movement, desc = "Не даёт вас за таргетить на элитрах")
public class AntiTarget extends Module {

    public static SliderSetting gradus = new SliderSetting("Наклон", 40f, 30f, 50f, 1f);
    public static SliderSetting speedantitarget = new SliderSetting("Скорость", 1.96f, 1.9f, 2.7f, 0.01f);

    public AntiTarget() {
        addSettings(gradus, speedantitarget);
    }

    @Override
    public boolean onEvent(Event event) {
        if (Manager.FUNCTION_MANAGER.auraFunction.target != null) {
            return false;
        }

        if (mc.player.isElytraFlying()) {
            float targetPitch = -gradus.getValue().floatValue();
            mc.player.rotationPitch = targetPitch;
        }
        return false;
    }
}