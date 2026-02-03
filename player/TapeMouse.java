package nuclear.module.impl.player;

import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.misc.TimerUtil;

@Annotation(
        name = "TapeMouse",
        type = TypeList.Player, desc = "Авто кликает за тебя или каждую тика"
)

public class TapeMouse extends Module {
    public SliderSetting speed = new SliderSetting("Скорость", 1, 0, 50, 1);
    public ModeSetting mode = new ModeSetting("Выбор кнопки", "Левая кнопка", new String[]{"Левая кнопка", "Правая кнопка"});
    private final TimerUtil timerUtil = new TimerUtil();
    public TapeMouse() {
        addSettings(mode, speed);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate e) {
            if (mode.is("Левая кнопка")) {
                if (timerUtil.hasTimeElapsed(speed.getValue().longValue() * 100L)) {
                    mc.clickMouse();
                    timerUtil.reset();
                }
            }
            if (mode.is("Правая кнопка")) {
                if (timerUtil.hasTimeElapsed(speed.getValue().longValue() * 100L)) {
                    mc.rightClickMouse();
                    timerUtil.reset();
                }
            }
        }

        return false;
    }
}
