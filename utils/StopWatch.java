package nuclear.utils;

public class StopWatch {
    public long lastMS = System.currentTimeMillis();
    private long startTime;

    public StopWatch() {
    }

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public boolean isReached(long time) {
        return System.currentTimeMillis() - this.lastMS > time;
    }

    public void setLastMS(long newValue) {
        this.lastMS = System.currentTimeMillis() + newValue;
    }

    public void resetes() {
        this.startTime = System.currentTimeMillis();
    }

    public void setTime(long time) {
        this.lastMS = time;
    }

    public long getTime() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public boolean isRunning() {
        return System.currentTimeMillis() - this.lastMS <= 0L;
    }

    public boolean hasTimeElapsed() {
        return this.lastMS < System.currentTimeMillis();
    }

    public long getLastMS() {
        return this.lastMS;
    }

    public boolean finished(final double delay) {
        return System.currentTimeMillis() - delay >= startTime;
    }
}