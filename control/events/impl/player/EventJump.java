package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nuclear.control.events.Event;

@Getter
@Setter
@AllArgsConstructor
public class EventJump extends Event {
    private float motion, yaw;
}
