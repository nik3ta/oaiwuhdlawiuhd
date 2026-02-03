package nuclear.module.impl.other;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(name = "NoCommands", type = TypeList.Other, desc = "Отключает команды чита такие как баритон и тд")
public class NoCommands extends Module {

    public NoCommands() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
