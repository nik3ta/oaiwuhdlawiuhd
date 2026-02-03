package nuclear.module.impl.movement;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(
        name = "Sprint",
        type = TypeList.Movement,
        desc = "Автоматически включает бег при движении вперед"
)
public class Sprint extends Module {

    public Sprint() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}
