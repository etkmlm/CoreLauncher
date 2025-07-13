package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.ui.entity.animation.BorderColorAnimation;
import com.laeben.corelauncher.ui.entity.animation.ColorAnimation;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class CField extends TextField {

    private ColorAnimation focusedAnimation;
    private final Rectangle n;

    private boolean enableBorderTracking;

    public CField() {
        getStyleClass().add("cfield");

        n = new Rectangle();
        n.setManaged(false);
        n.fillProperty().addListener(this::onChangeListener);
        getChildren().add(n);
        n.setStyle("-fx-fill: -control-focused-border-fill");


        focusedProperty().addListener(a -> {
            if (focusedAnimation == null)
                return;
            if (isFocused())
                focusedAnimation.playFromStart();
            else{
                focusedAnimation.play();
                focusedAnimation.jumpTo(Duration.ZERO);
                focusedAnimation.stop();
            }
        });
    }

    private void onChangeListener(ObservableValue<? extends Paint> a, Paint b, Paint np){
        if (enableBorderTracking && focusedAnimation != null && np instanceof Color c)
            focusedAnimation.setColor(c);
    }

    public void setFocusedAnimation(Duration d){
        focusedAnimation = new BorderColorAnimation();
        focusedAnimation.setNode(this);
        focusedAnimation.setDuration(d);
        enableBorderTracking = true;
    }

    public void setFocusedAnimation(Color c, Duration d){
        focusedAnimation = new BorderColorAnimation();
        focusedAnimation.setColor(c);
        focusedAnimation.setNode(this);
        focusedAnimation.setDuration(d);
    }
}
