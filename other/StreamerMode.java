package nuclear.module.impl.other;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

@Annotation(name = "StreamerMode", type = TypeList.Other, desc = "Скрывает некоторую информацию игры")
public class StreamerMode extends Module {
    public static final BooleanSetting hidenGrief = new BooleanSetting("Скрывать анархию/гриф", true);
    public static final BooleanSetting hidenCord = new BooleanSetting("Скрывать координаты", false);
    public static final BooleanSetting hidenBanner = new BooleanSetting("Скрывать баннеры", true);

    public StreamerMode() {
        addSettings(hidenGrief, hidenCord, hidenBanner);
    }

    @Override
    public boolean onEvent(Event event) {

        return false;
    }
}
