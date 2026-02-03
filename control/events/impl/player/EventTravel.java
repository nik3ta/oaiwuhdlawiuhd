package nuclear.control.events.impl.player;

import nuclear.control.events.Event;

public class EventTravel extends Event {

    public float speed;

    public EventTravel(float speed) {
        this.speed = speed;
    }

}
