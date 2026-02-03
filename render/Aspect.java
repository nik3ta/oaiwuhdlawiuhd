package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventAspectRatio;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "Aspect", type = TypeList.Render, desc = "Растягивает ваш экран")
public class Aspect extends Module {

    public final ModeSetting mode = new ModeSetting("Соотношение", "Кастомное", "4:3", "16:9", "1:1", "16:10", "Кастомное");
    public final SliderSetting aspectRatio = new SliderSetting("Значение", 1.6f, 0.1f, 5, 0.1f).setVisible(() -> mode.is("Кастомное"));

    public Aspect() {
        addSettings(mode, aspectRatio);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventAspectRatio e) {
            onAspectRatio(e);
        }
        return false;
    }

    public void onAspectRatio(EventAspectRatio e) {
        float value = switch (mode.get()) {
            case "4:3" -> 4.0f / 3.0f;
            case "16:9" -> 16.0f / 9.0f;
            case "1:1" -> 1.0f;
            case "16:10" -> 16.0f / 10.0f;
            default -> aspectRatio.getValue().floatValue();
        };

        e.setAspectRatio(value);
    }
}

