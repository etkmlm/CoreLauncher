package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.ui.entity.animation.BackgroundColorAnimation;
import com.laeben.corelauncher.ui.entity.animation.ColorAnimation;
import javafx.animation.*;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class CButton extends Button {

    //private static final BackgroundFill DEFAULT_HOVER_BACKGROUND = new BackgroundFill(, new CornerRadii(15), null);

    private final FadeTransition fade;
    private final FadeTransition fadeDeeper;
    private final ColorAnimation backgroundAnimation;

    private boolean trns;
    private Tooltip tip;
    public void setTooltip(String text){
        if (tip == null){
            super.setTooltip(tip = new Tooltip(text));
        }
        else
            tip.setText(text);
    }

    // no transparent animation
    private boolean nta = true;

    public CButton(){
        getStyleClass().add("cbutton");

        fade = new FadeTransition();
        fadeDeeper = new FadeTransition();
        fade.setNode(this);
        fadeDeeper.setNode(this);
        fade.setToValue(0.7);
        fade.setFromValue(1);
        fadeDeeper.setFromValue(1);
        fadeDeeper.setToValue(0.5);
        fade.setDuration(new Duration(200));
        fadeDeeper.setDuration(new Duration(300));

        backgroundAnimation = new BackgroundColorAnimation();
        backgroundAnimation.setColor(Color.rgb(2, 2, 2));
        backgroundAnimation.setDuration(Duration.millis(200));
        backgroundAnimation.setNode(this);

        setOnMouseEntered((a) -> {
            if (trns = (getBackground() == null || (!getBackground().getFills().isEmpty() && !getBackground().getFills().get(0).getFill().isOpaque())) && !nta){
                backgroundAnimation.playFromStart();
            }
            else
                fade.playFromStart();
        });

        setOnMouseExited((a) -> {
            if (trns && !nta){
                backgroundAnimation.play();
                backgroundAnimation.jumpTo(Duration.ZERO);
                backgroundAnimation.stop();
            }
            else {
                fade.play();
                fade.jumpTo(Duration.ZERO);
                fade.stop();
            }
        });

        setOnMousePressed((a) -> fadeDeeper.playFromStart());

        setOnMouseReleased((a) -> {
            fadeDeeper.play();
            fadeDeeper.jumpTo(Duration.ZERO);
            fadeDeeper.stop();
        });
    }

    public void enableTransparentAnimation(){
        nta = false;
    }

    /*public void setBackgroundImage(Image img, double radius){
        var image = new BackgroundImage(img, null, null, null, null);
        var rect = new Rectangle(img.getWidth(), img.getHeight());
        rect.setArcWidth(radius);
        rect.setArcHeight(radius);
        setClip(rect);
        getBackground().getImages().add(image);
        //setBackground(new Background(image));
    }*/


}
