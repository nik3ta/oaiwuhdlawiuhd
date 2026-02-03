package nuclear.module.impl.movement;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.InfoSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.module.settings.imp.SliderSetting;

@Annotation(name = "ElytraBooster", type = TypeList.Movement, desc = "Ускоряет полёт на элитрах")
public class ElytraBooster extends Module {
    public static ModeSetting mode = new ModeSetting("Режим", "Кастомный", "Кастомный", "Bravo", "ReallyWorld");

    public static final BooleanSetting maxspeed = new BooleanSetting("Скорость по Углам", false).setVisible(() -> mode.is("Кастомный"));
    public static SliderSetting speedxz = new SliderSetting("Скорость XZ", 1.65f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && !maxspeed.get()));
    public static SliderSetting speedy = new SliderSetting("Скорость Y", 1.59f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && !maxspeed.get()));

    public InfoSetting speedxzmax = new InfoSetting("Скорость по XZ", () -> {
    }).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));

    public static SliderSetting speed5 = new SliderSetting("Угол 0-5", 1.6f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed10 = new SliderSetting("Угол 5-10", 1.62f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed15 = new SliderSetting("Угол 10-15", 1.65f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed20 = new SliderSetting("Угол 15-20", 1.68f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed25 = new SliderSetting("Угол 20-25", 1.74f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed30 = new SliderSetting("Угол 25-30", 1.8f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed35 = new SliderSetting("Угол 30-35", 1.8f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed40 = new SliderSetting("Угол 35-40", 1.8f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));

    public InfoSetting speedymax = new InfoSetting("Угол по Y", () -> {
    }).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));

    public static SliderSetting speed5y = new SliderSetting("Угoл 0-5", 1.59f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed10y = new SliderSetting("Угoл 5-10", 1.6f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed15y = new SliderSetting("Угoл 10-15", 1.61f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed20y = new SliderSetting("Угoл 15-20", 1.62f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed25y = new SliderSetting("Угoл 20-25", 1.68f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed30y = new SliderSetting("Угoл 25-30", 1.74f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed35y = new SliderSetting("Угoл 30-35", 1.95f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));
    public static SliderSetting speed40y = new SliderSetting("Угoл 35-40", 2f, 1.5f, 2.5f, 0.01f).setVisible(() -> (mode.is("Кастомный") && maxspeed.get()));

    public ElytraBooster() {
        this.addSettings(mode, maxspeed, speedxz, speedy, speedxzmax, speed5, speed10, speed15, speed20, speed25, speed30, speed35, speed40, speedymax, speed5y, speed10y, speed15y, speed20y, speed25y, speed30y, speed35y, speed40y);
    }

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}