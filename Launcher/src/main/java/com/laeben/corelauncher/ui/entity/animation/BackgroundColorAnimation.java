package com.laeben.corelauncher.ui.entity.animation;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class BackgroundColorAnimation extends ColorAnimation{

    private CornerRadii radii;

    private CornerRadii tryGetRadius(Region node){
        if (radii != null){
            return radii;
        }

        return node.getBackground() != null && !node.getBackground().getFills().isEmpty() ? (radii = node.getBackground().getFills().get(0).getRadii()) : null;
    }

    @Override
    protected void interpolateColor(Region node, Color color) {
        node.setBackground(new Background(new BackgroundFill(color, tryGetRadius(node), null)));
    }
}
