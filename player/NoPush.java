package nuclear.module.impl.player;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;


@Annotation(name = "NoPush", type = TypeList.Player)
public class NoPush extends Module {

    public final MultiBoxSetting modes = new MultiBoxSetting("Тип",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Блоки", true),
            new BooleanSetting("Удочка", true));

    public NoPush() {
        addSettings(modes);
    }

    @Override
    public boolean onEvent(final Event event) {
        return false;
    }
}
