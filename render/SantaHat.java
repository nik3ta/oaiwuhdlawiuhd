package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;

@Annotation(name = "SantaHat", type = TypeList.Render, desc = "Нового года не будэ, дед мороз принял ислам")
public class SantaHat extends Module {

    public final MultiBoxSetting entity = new MultiBoxSetting("Выбор целей",
            new BooleanSetting("Игроки", false),
            new BooleanSetting("Себя", true),
            new BooleanSetting("Друзья", true));

    public SantaHat() {
        addSettings(entity);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
