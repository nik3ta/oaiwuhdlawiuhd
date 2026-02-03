package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ModeSetting;

@Annotation(name = "CustomDance", type = TypeList.Render, desc = "Изменяет анимацию танцев клиентов жителей")
public class CustomDance extends Module {

    public ModeSetting modes = new ModeSetting("Тип", "Дрочка", "Дрочка", "Наруто");

    public CustomDance() {
        addSettings(modes);
    }


    @Override
    public boolean onEvent(final Event event) {
        return false;
    }
}
