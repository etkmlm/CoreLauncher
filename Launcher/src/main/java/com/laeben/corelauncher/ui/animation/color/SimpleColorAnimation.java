package com.laeben.corelauncher.ui.animation.color;

import javafx.css.StyleableObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class SimpleColorAnimation extends ColorAnimation {
    private StyleableObjectProperty<Paint> property;

    public void setProperty(StyleableObjectProperty<Paint> property){
        this.property = property;
    }

    @Override
    protected void interpolateColor(Color color) {
        if (this.property != null && !this.property.isBound()) this.property.set(color);
    }
}
