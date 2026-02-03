package nuclear.module.impl.player;

import nuclear.control.events.Event;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;

@Annotation(name = "ScoreboardHealth", type = TypeList.Player, desc = "Обход показа хп где хп игроков скрыто")
public class ScoreboardHealth extends Module {

    @Override
    public boolean onEvent(Event event) {
        return false;
    }
}
