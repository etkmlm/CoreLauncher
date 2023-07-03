package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.ui.controls.CButton;
import com.cdev.corelauncher.ui.entities.LStage;
import com.cdev.corelauncher.ui.utils.FXManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Frame {
    @FXML
    private CButton btnClose;
    @FXML
    private CButton btnMinimize;
    @FXML
    private CButton btnMaximize;
    @FXML
    private StackPane content;
    @FXML
    private AnchorPane root;
    @FXML
    private Label lblTitle;

    public void setTitle(String title){
        lblTitle.setText(title);
    }
    @FXML
    private void initialize(){
        btnClose.setOnMouseClicked(a -> FXManager.getManager().closeStage(getStage()));
        btnMinimize.setOnMouseClicked(a -> getStage().setIconified(true));
        btnMaximize.setOnMouseClicked(a -> getStage().setMaximized(!getStage().isMaximized()));
        //content.setClip(root);
    }

    private LStage getStage(){
        return (LStage) content.getScene().getWindow();
    }
}
