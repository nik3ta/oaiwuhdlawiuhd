package nuclear.module.impl.movement;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "ElytraMotion", type = TypeList.Movement, desc = "Позволяет зависнуть возле цели на элитрах")
public class ElytraMotion extends Module {

    public final SliderSetting distancie = new SliderSetting("Дист до таргета", 2F, 1.25F, 2.5F, 0.05F);

    public ElytraMotion() {
        addSettings(distancie);
    }

    @Override
    public boolean onEvent(final Event event) {
        return false;
    }
}
