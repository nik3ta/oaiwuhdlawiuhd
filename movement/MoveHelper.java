package nuclear.module.impl.movement;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

@Annotation(name = "MoveHelper", type = TypeList.Movement, desc = "Ускоряет ваше движение в определённых моментах")
public class MoveHelper extends Module {
    public final BooleanSetting lader = new BooleanSetting("Ускорение на лестницах", true);
    public final BooleanSetting slime = new BooleanSetting("Ускорение на слизи", true);
    public final BooleanSetting med = new BooleanSetting("Ускорение на мёде", true);

    public MoveHelper() {
        addSettings(lader, slime, med);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}