package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

@Annotation(name = "ClickGui", type = TypeList.Render)
public class ClickGui extends Module {
    public final BooleanSetting sounds = new BooleanSetting("Звуки кнопок", true);

    public ClickGui() {
        addSettings(sounds);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        setState(false);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
