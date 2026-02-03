package nuclear.control.handler;

import nuclear.control.events.EventManager;
import nuclear.control.handler.impl.TargetESPHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HandlerManager {

    private final List<Object> handlers = new CopyOnWriteArrayList<>();

    public void init() {
        add(new TargetESPHandler());
    }

    public void add(Object handler) {
        handlers.add(handler);
        EventManager.register(handler);
    }
}

