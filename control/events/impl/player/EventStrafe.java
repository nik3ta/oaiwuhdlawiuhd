package nuclear.control.events.impl.player;

import nuclear.control.events.Event;

public class EventStrafe extends Event {

    public float yaw;

    public EventStrafe(float yaw) {
        this.yaw = yaw;
    }

}
