package com.laeben.corelauncher.ui.animation.color;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public abstract class RegionColorAnimation extends ColorAnimation {
    private Region node;

    public void setRegion(Region node){
        this.node = node;
    }

    @Override
    protected final void interpolateColor(Color color){
        interpolateColor(node, color);
    }

    protected abstract void interpolateColor(Region node, Color color);
}
