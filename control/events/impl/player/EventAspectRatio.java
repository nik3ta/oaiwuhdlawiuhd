package nuclear.control.events.impl.player;

import lombok.Getter;
import nuclear.control.events.Event;

@Getter
public class EventAspectRatio extends Event {
    private float aspectRatio;

    public EventAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}