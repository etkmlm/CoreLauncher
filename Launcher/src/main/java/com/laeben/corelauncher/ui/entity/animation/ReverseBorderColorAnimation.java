package com.laeben.corelauncher.ui.entity.animation;

import javafx.animation.Interpolator;

public class ReverseBorderColorAnimation extends BorderColorAnimation{
    private static class ReverseInterpolator extends Interpolator {
        private final double start, end;

        public ReverseInterpolator(double start, double end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected double curve(double t) {
            if (t >= start && t <= end)
                return 1;

            if (t < start)
                return 1.0 / start * t;
            else
                return 1.0 / end * (1 - t);
        }
    }

    public ReverseBorderColorAnimation() {
        setInterpolator(new ReverseInterpolator(0.45, 0.55));
    }

    public void setReverseDelay(int delay) {
        var perc = delay / getTotalDuration().toMillis();
        double min = (1 - perc) / 2;
        double max = min + perc;
        setInterpolator(new ReverseInterpolator(min, max));
    }

}
