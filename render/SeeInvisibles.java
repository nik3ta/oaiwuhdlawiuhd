package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(
        name = "SeeInvisibles",
        type = TypeList.Render
)

public class SeeInvisibles extends Module {
    public SliderSetting alpha = new SliderSetting("Прозрачность", 0.5F, 0.3F, 1.0F, 0.1F);
    public SeeInvisibles() {
        this.addSettings(alpha);
    }

    public boolean onEvent(Event event) {
        return false;
    }
}
