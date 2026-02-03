package nuclear.module.impl.render;

import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventOverlaysRender;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;

@Annotation(name = "NoRender", type = TypeList.Render, desc = "Убирает не нужный рендер")
public class NoRender extends Module {

    public MultiBoxSetting element = new MultiBoxSetting("Элементы",
            new BooleanSetting("Оверлей огня", true),
            new BooleanSetting("Плохие эффекты", true),
            new BooleanSetting("Босс бар", false),
            new BooleanSetting("Скорборд", false),
            new BooleanSetting("Заголовки", false),
            new BooleanSetting("Аним тотема", false),
            new BooleanSetting("Дождь", true),
            new BooleanSetting("Камера клип", true),
            new BooleanSetting("Затемнение фона", false),
            new BooleanSetting("Тряска от урона", true),
            new BooleanSetting("Размытие под водой", true),
            new BooleanSetting("Интерполяцию рук", true),
            new BooleanSetting("Размытие под лавой", true),
            new BooleanSetting("Зачарование предметов", false));

    public NoRender() {
        addSettings(element);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventOverlaysRender) {
            handleEventOverlaysRender((EventOverlaysRender) event);
        } else if (event instanceof EventUpdate) {
            handleEventUpdate((EventUpdate) event);
        }
        return false;
    }

    private void handleEventOverlaysRender(EventOverlaysRender event) {
        EventOverlaysRender.OverlayType overlayType = event.getOverlayType();

        boolean cancelOverlay = switch (overlayType) {
            case FIRE_OVERLAY -> element.get(0);
            case BOSS_LINE -> element.get(2);
            case SCOREBOARD -> element.get(3);
            case TITLES -> element.get(4);
            case TOTEM -> element.get(5);
        };

        if (cancelOverlay) {
            event.setCancel(true);
        }
    }

    private void handleEventUpdate(EventUpdate event) {

        boolean isRaining = element.get(6) && mc.world.isRaining();

        if (isRaining) {
            mc.world.setRainStrength(0);
            mc.world.setThunderStrength(0);
        }

    }
}
