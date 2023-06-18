package com.cdev.corelauncher.ui.controls;

import javafx.animation.FadeTransition;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

public class CButton extends Button {

    private static final BackgroundFill DEFAULT_BACKGROUND = new BackgroundFill(Paint.valueOf("#303030"), new CornerRadii(10), null);
    //private static final BackgroundFill DEFAULT_HOVER_BACKGROUND = new BackgroundFill(, new CornerRadii(15), null);

    private final FadeTransition fade;
    private final FadeTransition fadeDeeper;

    public CButton(){
        setBackground(new Background(DEFAULT_BACKGROUND));
        setTextFill(Paint.valueOf("white"));

        fade = new FadeTransition();
        fadeDeeper = new FadeTransition();
        fade.setNode(this);
        fadeDeeper.setNode(this);
        fade.setToValue(0.8);
        fade.setFromValue(1);
        fadeDeeper.setFromValue(1);
        fadeDeeper.setToValue(0.6);
        fade.setDuration(new Duration(200));
        fadeDeeper.setDuration(new Duration(300));

        setOnMouseEntered((a) -> {
            fade.playFromStart();
        });

        setOnMouseExited((a) -> {
            fade.play();
            fade.jumpTo(Duration.ZERO);
            fade.stop();
        });

        setOnMouseClicked((a) -> {
            fadeDeeper.playFromStart();
        });

        setOnMouseReleased((a) -> {
            fadeDeeper.play();
            fadeDeeper.jumpTo(Duration.ZERO);
            fadeDeeper.stop();
        });
    }


}
