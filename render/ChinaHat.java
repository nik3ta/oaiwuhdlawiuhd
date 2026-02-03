package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(name = "ChinaHat", type = TypeList.Render, desc = "Создаёт китайскую шляпу над головой")
public class ChinaHat extends Module {

    public ChinaHat() {
        this.addSettings();
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}