package com.cdev.corelauncher.ui.controls;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class CMsgBox extends Alert {

    public CMsgBox(AlertType alertType) {
        super(alertType);

        getDialogPane().getStylesheets().add(getClass().getResource("/com/cdev/corelauncher/style/controls/alert.css").toString());
    }

    public static CMsgBox msg(AlertType type, String title, String desc){

        return new CMsgBox(type).setInfo(title, desc);
    }

    public CMsgBox setButtons(ButtonType... types){
        getButtonTypes().clear();
        getButtonTypes().addAll(types);

        return this;
    }

    public CMsgBox setInfo(String title, String desc){
        setHeaderText(title);
        setTitle(title);
        setContentText(desc);
        return this;
    }
}
