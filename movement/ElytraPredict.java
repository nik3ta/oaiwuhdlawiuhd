package nuclear.module.impl.movement;

import net.minecraft.entity.LivingEntity;
import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.StopWatch;

@Annotation(
        name = "ElytraPredict",
        type = TypeList.Movement,
        desc = "Смещает хитбокс противника во время полёта на элитрах для перегона на элитрах"
)
public class ElytraPredict extends Module {
    public final SliderSetting elytradistance = new SliderSetting("Дистанция обгона", 3.0F, 0.0F, 4.5F, 0.05F);
    public final StopWatch timer = new StopWatch();
    public boolean disabled = false;

    public ElytraPredict() {
        this.addSettings(elytradistance);
    }

    public boolean onEvent(Event event) {
        return false;
    }

    public double getElytraDistance(LivingEntity target) {
        return elytradistance.getValue().floatValue();
    }

    public boolean canPredict(LivingEntity target) {
        if (mc.player.hurtTime > 0 && !target.lastSwing.finished(500)) {
            disabled = true;
            timer.resetes();
        }
        if (timer.finished(500)) disabled = false;
        return !disabled;
    }
}