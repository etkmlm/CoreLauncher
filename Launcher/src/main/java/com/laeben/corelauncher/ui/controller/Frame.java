package com.laeben.corelauncher.ui.controller;

import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.ui.control.CShapefulButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

public class Frame extends Controller implements com.laeben.corelauncher.api.ui.entity.Frame {
    public static final String TRANSPARENT_FRAME_CLASS = "transparent-frame";

    @FXML
    private AnchorPane frameRoot;
    @FXML
    private CShapefulButton btnClose;
    @FXML
    private CShapefulButton btnMinimize;
    @FXML
    private CShapefulButton btnMaximize;
    @FXML
    private StackPane content;

    private final BooleanProperty transparency;

    public Frame(){
        transparency = new SimpleBooleanProperty();

        transparency.addListener((observable, oldValue, newValue) -> {
            if (frameRoot == null) return;

            if (newValue) {
                if (!frameRoot.getStyleClass().contains(TRANSPARENT_FRAME_CLASS))
                    frameRoot.getStyleClass().add(TRANSPARENT_FRAME_CLASS);
            }
            else frameRoot.getStyleClass().remove(TRANSPARENT_FRAME_CLASS);
        });
    }


    public void setTitle(String title){
        //
    }

    @Override
    public void setContent(Node node) {
        content.getChildren().clear();
        content.getChildren().add(node);
    }

    public BooleanProperty transparencyProperty(){
        return transparency;
    }
    public boolean getTransparency(){
        return transparency.get();
    }
    public void setTransparency(boolean value){
        transparency.set(value);
    }

    @Override
    public void preInit(){
        btnClose.setOnMouseClicked(a -> close());
        btnMinimize.setOnMouseClicked(a -> getStage().setIconified(true));
        btnMaximize.setOnMouseClicked(a -> getStage().setMaximized(!getStage().isMaximized()));
    }
}
