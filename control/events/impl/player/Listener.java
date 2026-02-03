package nuclear.control.events.impl.player;


import nuclear.control.events.Event;

public interface Listener<T extends Event> {
    void onEvent(T var1);
}

