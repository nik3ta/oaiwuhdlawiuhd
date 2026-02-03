package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

import static nuclear.module.TypeList.Render;

@Annotation(name = "ItemPhysics", type = Render, desc = "Добавляет физику предметом при выбрасывание")
public class ItemPhysics extends Module {

    public final BooleanSetting size = new BooleanSetting("Уменьшить предметы", true);

    public ItemPhysics() {
        addSettings(size);
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}

