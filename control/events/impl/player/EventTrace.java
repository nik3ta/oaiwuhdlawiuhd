package nuclear.control.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nuclear.control.events.Event;

@Setter
@Getter
@AllArgsConstructor
public class EventTrace extends Event {

    private float yaw, pitch;

}

