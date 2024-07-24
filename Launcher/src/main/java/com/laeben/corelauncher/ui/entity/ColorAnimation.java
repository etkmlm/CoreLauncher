package com.laeben.corelauncher.ui.entity;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public abstract class ColorAnimation extends Transition {

    private int r,g,b;
    private Region node;

    public ColorAnimation() {
        setInterpolator(Interpolator.EASE_IN);
    }

    public void setColor(Color c) {
        this.r = (int)(c.getRed() * 255);
        this.g = (int)(c.getGreen() * 255);
        this.b = (int)(c.getBlue() * 255);
    }

    public void setNode(Region node) {
        this.node = node;
    }

    public void setDuration(Duration duration) {
        setCycleDuration(duration);
    }

    @Override
    protected void interpolate(double frac) {
        var color = Color.rgb(r, g, b, frac / 2);
        interpolateColor(node, color);
    }

    protected abstract void interpolateColor(Region node, Color color);
}
