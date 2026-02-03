package nuclear.utils.anim;


import nuclear.utils.misc.TimerUtil;

/**
 * This animation superclass was made by Foggy and advanced by cedo
 *
 * @author Foggy
 * @author cedo
 * @since 7/21/2020 (yes 2020)
 * @since 7/29/2021
 */
public abstract class Animation {

    public TimerUtil timerUtil = new TimerUtil();
    protected int duration;
    protected double endPoint;
    protected Direction direction;
    protected long startTime;
    protected double startPoint;

    public Animation(int ms, double endPoint) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.startPoint = 0;
        this.direction = Direction.FORWARDS;
        this.startTime = System.currentTimeMillis();
    }


    public Animation(int ms, double endPoint, Direction direction) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.startPoint = 0;
        this.direction = direction;
        this.startTime = System.currentTimeMillis();
    }

    public Animation(int ms, double endPoint, double startPoint) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.startPoint = startPoint;
        this.direction = Direction.FORWARDS;
        this.startTime = System.currentTimeMillis();
    }

    public boolean finished(Direction direction) {
        return isDone() && this.direction.equals(direction);
    }

    public double getLinearOutput() {
        return 1 - ((timerUtil.getTime() / (double) duration) * endPoint);
    }

    public double getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(double endPoint) {
        this.endPoint = endPoint;
    }

    public void reset() {
        timerUtil.reset();
    }

    public boolean isDone() {
        return timerUtil.hasTimeElapsed(duration);
    }

    public void changeDirection() {
        setDirection(direction.opposite());
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            timerUtil.setTime(System.currentTimeMillis() - (duration - Math.min(duration, timerUtil.getTime())));
        }
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    protected boolean correctOutput() {
        return false;
    }
    public double getOutput2() {
        long currentTime = System.currentTimeMillis();
        double progress = Math.min(1.0, (currentTime - startTime) / (double) duration);
        if (direction == Direction.BACKWARDS) {
            progress = 1.0 - progress;
        }
        return startPoint + (endPoint - startPoint) * getEquation(progress);
    }
    public double getOutput() {
        if (direction == Direction.FORWARDS) {
            if (isDone())
                return endPoint;
            return (getEquation(timerUtil.getTime()) * endPoint);
        } else {
            if (isDone()) return 0;
            if (correctOutput()) {
                double revTime = Math.min(duration, Math.max(0, duration - timerUtil.getTime()));
                return getEquation(revTime) * endPoint;
            } else return (1 - getEquation(timerUtil.getTime())) * endPoint;
        }
    }

    protected abstract double getEquation(double x);

}

