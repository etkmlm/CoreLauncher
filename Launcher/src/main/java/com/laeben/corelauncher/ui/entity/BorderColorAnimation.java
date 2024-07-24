package com.laeben.corelauncher.ui.entity;

import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class BorderColorAnimation extends ColorAnimation{

    private BorderWidths widths;
    private CornerRadii radii;

    private BorderWidths tryGetWidth(Region node){
        if (widths != null)
            return widths;
        return node.getBorder() != null && !node.getBorder().getStrokes().isEmpty() ? (widths = node.getBorder().getStrokes().get(0).getWidths()) : new BorderWidths(1.5);
    }

    private CornerRadii tryGetRadius(Region node){
        if (radii != null)
            return radii;
        return node.getBorder() != null && !node.getBorder().getStrokes().isEmpty() ? (radii = node.getBorder().getStrokes().get(0).getRadii()) : null;
    }

    @Override
    protected void interpolateColor(Region node, Color color) {
        node.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, tryGetRadius(node), tryGetWidth(node))));
    }
}
