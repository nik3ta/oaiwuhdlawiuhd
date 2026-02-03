package nuclear.control.events.impl.game;

import nuclear.control.events.Event;

public class EventKey extends Event {

    public int key;

    public EventKey(int key) {
        this.key = key;
    }
}
