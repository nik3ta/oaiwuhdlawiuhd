/*
 * Decompiled with CFR 0.153-SNAPSHOT (d6f6758-dirty).
 */
package nuclear.utils.anim.animations;

import nuclear.utils.anim.Animation;
import nuclear.utils.anim.Direction;

public class DecelerateAnimation
extends Animation {
    public DecelerateAnimation(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public DecelerateAnimation(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    @Override
    protected double getEquation(double x) {
        double x1 = x / (double)this.duration;
        return 1.0 - (x1 - 1.0) * (x1 - 1.0);
    }
}

