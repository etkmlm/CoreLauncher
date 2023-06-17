package com.cdev.corelauncher.ui.entities;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class LScene extends Scene {
    private final FXMLLoader loader;
    public LScene(Parent root, int w, int h, FXMLLoader loader) {
        super(root);

        this.loader = loader;
    }

    public LScene(Parent root, FXMLLoader loader) {
        super(root);

        this.loader = loader;
    }

    public <T> T getController(){
        return loader.getController();
    }
}
