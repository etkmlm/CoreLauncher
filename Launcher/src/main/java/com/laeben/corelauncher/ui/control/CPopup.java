package com.laeben.corelauncher.ui.control;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.util.Duration;

public class CPopup extends Popup {
    private final ScaleTransition trnsHorizontal;
    private Timeline trnsVertical;

    private Node content;

    private boolean vertical = false;

    private int duration;

    public CPopup(){
        setAutoHide(true);

        trnsHorizontal = new ScaleTransition();
        trnsHorizontal.setFromX(0);
        trnsHorizontal.setToX(1);

        setDuration(2000);
    }

    public void setDirection(boolean v){
        vertical = v;
    }

    public void setDuration(int d){
        duration = d;
        trnsHorizontal.setDuration(Duration.millis(d));
    }

    public void setContent(Node content){
        this.content = content;

        getContent().clear();
        getContent().add(content);

        if (vertical && content instanceof Region r){
            trnsVertical = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(r.maxHeightProperty(), 1)),
                    new KeyFrame(Duration.millis(duration), new KeyValue(r.maxHeightProperty(), 200, Interpolator.LINEAR))
            );
        }
        else
            trnsHorizontal.setNode(content);
    }

    public void show(Node owner, double x, double y){
        if (vertical){
            trnsVertical.playFromStart();
        }
        else{
            content.setScaleX(0);
            trnsHorizontal.playFromStart();
        }

        super.show(owner, x, y);
    }
}
