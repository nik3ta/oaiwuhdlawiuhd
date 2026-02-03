package nuclear.utils.anim.impl;


import nuclear.utils.anim.Animation;
import nuclear.utils.anim.Direction;

public class DecelerateAnimation extends Animation {

    public DecelerateAnimation(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public DecelerateAnimation(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    public DecelerateAnimation(int ms, double endPoint, double startPoint) {
        super(ms, endPoint, startPoint);
    }

    @Override
    protected double getEquation(double x) {
        double x1 = x / duration;
        return 1 - ((x1 - 1) * (x1 - 1));
    }
}