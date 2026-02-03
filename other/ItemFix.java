package nuclear.module.impl.other;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

@Annotation(name = "ItemFix", type = TypeList.Other)
public class ItemFix extends Module {

    public BooleanSetting placeManually = new BooleanSetting("Не ставить шары", true);


    public ItemFix() {
        addSettings(placeManually);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}

