package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.ui.entity.BorderColorAnimation;
import com.laeben.corelauncher.ui.entity.ColorAnimation;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class CField extends TextField {

    private ColorAnimation focusedAnimation;

    public CField() {

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

    public void setFocusedAnimation(Color c, Duration d){
        focusedAnimation = new BorderColorAnimation();
        focusedAnimation.setColor(c);
        focusedAnimation.setNode(this);
        focusedAnimation.setDuration(d);
    }
}
