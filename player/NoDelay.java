package nuclear.module.impl.player;

import net.minecraft.item.Items;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.Setting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.misc.TimerUtil;

@Annotation(name = "NoDelay", type = TypeList.Player)
public class NoDelay extends Module {

    private final MultiBoxSetting actions = new MultiBoxSetting("Действия", new BooleanSetting[]{new BooleanSetting("Прыжки", true), new BooleanSetting("Правая кнопка", false)});
    private final SliderSetting jumpticks = new SliderSetting("Задержка прыжка", 0.1F, 0.0F, 2F, 0.1F).setVisible(() -> actions.get(0));
    private final SliderSetting clickticks = new SliderSetting("Задержка клика", 0.1F, 0.0F, 2F, 0.1F).setVisible(() -> actions.get(1));
    public BooleanSetting actions1 = new BooleanSetting("Только с опытом", false).setVisible(() -> actions.get(1));
    public BooleanSetting actions2 = new BooleanSetting("Обход Grim", true).setVisible(() -> actions.get(1));
    private final TimerUtil timerUtil = new TimerUtil();

    public NoDelay() {
        this.addSettings(new Setting[]{this.actions, this.jumpticks, this.clickticks, this.actions1, this.actions2});
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (this.actions.get(0)) {
                mc.player.jumpTicks = this.jumpticks.getValue().intValue();
            }

            if (this.actions.get(1)) {
                if (!this.actions1.get() && !this.actions2.get()) {
                    mc.rightClickDelayTimer = this.clickticks.getValue().intValue();
                }

                if (this.actions1.get() && (mc.player.getHeldItemMainhand().getItem() == Items.EXPERIENCE_BOTTLE || mc.player.getHeldItemOffhand().getItem() == Items.EXPERIENCE_BOTTLE)) {
                    mc.rightClickDelayTimer = this.clickticks.getValue().intValue();
                }

                if (this.actions2.get()) {
                    if (this.actions1.get() && (mc.player.getHeldItemMainhand().getItem() == Items.EXPERIENCE_BOTTLE || mc.player.getHeldItemOffhand().getItem() == Items.EXPERIENCE_BOTTLE)) {
                        mc.rightClickDelayTimer = 1;
                    }

                    if (!this.actions1.get() && this.timerUtil.hasTimeElapsed(55L)) {
                        mc.rightClickDelayTimer = 1;
                        this.timerUtil.reset();
                    }
                }
            }
        }

        return false;
    }
}
