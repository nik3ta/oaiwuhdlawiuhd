package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(name = "Chams", type = TypeList.Render)
public class Chams extends Module {

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}


