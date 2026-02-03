package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nuclear.control.events.Event;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class EventOverlaysRender extends Event {

    private final OverlayType overlayType;

    public enum OverlayType {
        FIRE_OVERLAY, BOSS_LINE, SCOREBOARD, TITLES, TOTEM
    }
}
