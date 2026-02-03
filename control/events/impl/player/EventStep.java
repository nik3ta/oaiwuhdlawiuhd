package nuclear.control.events.impl.player;

import nuclear.control.events.Event;

public class EventStep extends Event {

    public float stepHeight;

    public EventStep(float stepHeight) {
        this.stepHeight = stepHeight;
    }

}
