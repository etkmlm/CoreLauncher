package com.laeben.corelauncher.ui.entity;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class LScene<T> extends Scene {
    private final T controller;
    private LStage stage;

    public LScene(Parent root, T controller) {
        super(root);

        this.controller = controller;
    }

    public LScene setStage(LStage stage){
        this.stage = stage;

        return this;
    }
    public T getController(){
        return controller;
    }
}
