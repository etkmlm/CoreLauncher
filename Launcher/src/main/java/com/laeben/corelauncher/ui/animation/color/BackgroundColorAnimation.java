package com.laeben.corelauncher.ui.animation.color;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.List;

public class BackgroundColorAnimation extends RegionColorAnimation {

    private CornerRadii radii;

    private CornerRadii tryGetRadius(Region node){
        if (radii != null){
            return radii;
        }

        return node.getBackground() != null && !node.getBackground().getFills().isEmpty() ? (radii = node.getBackground().getFills().get(0).getRadii()) : null;
    }

    public void setRadii(CornerRadii radii){
        this.radii = radii;
    }

    @Override
    protected void interpolateColor(Region node, Color color) {
        node.setBackground(new Background(List.of(new BackgroundFill(color, tryGetRadius(node), null)), node.getBackground() == null ? null : node.getBackground().getImages()));
    }
}
