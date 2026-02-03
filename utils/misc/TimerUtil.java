package nuclear.utils.misc;

public class TimerUtil {
    public long lastMS = System.currentTimeMillis();

    public boolean isReached(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }


    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - lastMS > time) {
            if (reset) reset();
            return true;
        }

        return false;
    }

    public boolean finished(double time) {
        return (System.currentTimeMillis() - this.lastMS) > time;
    }

    public long getLastMS() {
        return this.lastMS;
    }

    public void setLastMC() {
        lastMS = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public void setTime(long time) {
        lastMS = time;
    }

    public long getTimeElapsed() {
        return System.currentTimeMillis() - lastMS;
    }

    public boolean passed(float time) {
        return passed((long) time);
    }

    public boolean passed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }
}
