package nuclear.control.events;

import nuclear.control.Manager;
import nuclear.module.api.Module;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static nuclear.utils.IMinecraft.mc;

public class EventManager {

    private static final List<EventHandler> handlers = new CopyOnWriteArrayList<>();
    private static final Map<Class<?>, List<EventHandler>> eventHandlerCache = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private record EventHandler(Object listener, MethodHandle handle, Class<?> eventType) {}

    public static void call(final Event event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (event.isCancel()) {
            return;
        }

        callEvent(event);
    }

    private static void callEvent(Event event) {
        List<Module> modules = Manager.FUNCTION_MANAGER.getFunctions();
        for (int i = 0, size = modules.size(); i < size; i++) {
            Module module = modules.get(i);
            if (module.isState()) {
                module.onEvent(event);
            }
        }

        Class<?> eventClass = event.getClass();
        List<EventHandler> cachedHandlers = eventHandlerCache.get(eventClass);

        if (cachedHandlers == null) {
            cachedHandlers = new ArrayList<>();
            for (int i = 0, size = handlers.size(); i < size; i++) {
                EventHandler handler = handlers.get(i);
                if (handler.eventType.isAssignableFrom(eventClass)) {
                    cachedHandlers.add(handler);
                }
            }
            eventHandlerCache.put(eventClass, cachedHandlers);
        }

        for (int i = 0, size = cachedHandlers.size(); i < size; i++) {
            EventHandler handler = cachedHandlers.get(i);
            try {
                handler.handle.invoke(handler.listener, event);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameters()[0].getType();
                if (Event.class.isAssignableFrom(paramType)) {
                    try {
                        method.setAccessible(true);
                        MethodHandle handle = LOOKUP.unreflect(method);
                        handlers.add(new EventHandler(listener, handle, paramType));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        eventHandlerCache.clear();
    }
}
