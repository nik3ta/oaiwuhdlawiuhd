package nuclear.module.impl.player;

import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventDeath;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(name = "AutoRespawn", type = TypeList.Player, desc = "Моментально жмёт кнопку возрождение")
public class AutoRespawn extends Module {

    @Override
    public boolean onEvent(final Event event) {
        if (event instanceof EventDeath) {
            death((EventDeath) event);
            return true;
        }
        return false;
    }

    public void death(EventDeath event) {
        if (mc.player == null) return;
        mc.player.respawnPlayer();
        mc.displayGuiScreen(null);
    }
}