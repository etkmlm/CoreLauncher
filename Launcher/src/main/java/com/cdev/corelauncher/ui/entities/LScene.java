package com.cdev.corelauncher.ui.entities;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class LScene<T> extends Scene {
    private final T controller;
    private LStage stage;
    private double xD;
    private double yD;


    public LScene(Parent root, T controller) {
        super(root);

        this.controller = controller;

        setOnMousePressed(a -> {
            xD = stage.getX() - a.getScreenX();
            yD = stage.getY() - a.getScreenY();
        });
        setOnMouseDragged(a -> {
            stage.setX(a.getScreenX() + xD);
            stage.setY(a.getScreenY() + yD);
        });
    }

    public LScene setStage(LStage stage){
        this.stage = stage;

        return this;
    }
    public T getController(){
        return controller;
    }
}
