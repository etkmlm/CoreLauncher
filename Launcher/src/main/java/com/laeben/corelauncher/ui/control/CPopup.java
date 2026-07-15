package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.CoreLauncherFX;
import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.util.Duration;

public class CPopup extends Popup {
    private final ScaleTransition trnsHorizontal;
    private Timeline trnsVertical;

    private Node content;
    private Region clipContent;

    private double relX;
    private double relY;

    private boolean vertical = false;

    private int duration;

    public CPopup(){
        setAutoHide(true);

        trnsHorizontal = new ScaleTransition();
        trnsHorizontal.setFromX(0);
        trnsHorizontal.setToX(1);

        setDuration(2000);
    }

    /**
     * Set popup animation direction.
     * @param v vertical or not (horizontal)
     */
    public void setDirection(boolean v){
        vertical = v;
    }

    public void setDuration(int d){
        duration = d;
        trnsHorizontal.setDuration(Duration.millis(d));
    }

    public void setContent(Parent content){
        this.content = content;
        getContent().clear();
        content.getStylesheets().add(CoreLauncherFX.CLUI_CSS);
        getContent().add(content);
        content.layoutBoundsProperty().addListener((a, b, c) -> {
            if (!isShowing()) return;
            setX(relX - c.getWidth());
            setY(relY - c.getHeight());
        });

        if (!vertical) trnsHorizontal.setNode(content);
    }

    public void showRelative(Node owner, double x, double y){
        var bounds = owner.localToScreen(owner.getBoundsInLocal());
        relX = bounds.getMinX() - x;
        relY = bounds.getMinY() - y;

        this.show(owner, relX - content.getLayoutBounds().getWidth(), relY - content.getLayoutBounds().getHeight());
    }

    public void show(Node owner, double x, double y){
        super.show(owner, x, y);
        if (vertical){
            if (content instanceof Region r){
                trnsVertical = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(r.maxHeightProperty(), 1)),
                        new KeyFrame(Duration.millis(duration), new KeyValue(r.maxHeightProperty(), content.getLayoutBounds().getHeight(), Interpolator.LINEAR))
                );
                trnsVertical.playFromStart();
                trnsVertical.setOnFinished(a -> setScrollBarOpacity(1));
            }
        }
        else{
            content.setScaleX(0);
            trnsHorizontal.playFromStart();
        }
        setScrollBarOpacity(0);
    }

    private Node cachedScrollBar = null;
    private void setScrollBarOpacity(double opacity){
        if (cachedScrollBar == null){
            for(var s : content.lookupAll(".scroll-bar")){
                cachedScrollBar = s;
                break;
            }
            if (cachedScrollBar == null) return;
        }

        cachedScrollBar.setOpacity(opacity);
    }
}
