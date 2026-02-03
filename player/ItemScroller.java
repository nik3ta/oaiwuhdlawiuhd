package nuclear.module.impl.player;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;



@Annotation(name = "ItemScroller", type = TypeList.Player)
public class ItemScroller extends Module {

    public SliderSetting delay = new SliderSetting("Задержка", 80, 0, 1000, 1);


    public ItemScroller() {
        addSettings(delay);
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}
