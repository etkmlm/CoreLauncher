package com.laeben.corelauncher.ui.controls;

import com.laeben.corelauncher.ui.entities.LStage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class CMsgBox extends Alert {

    public CMsgBox(AlertType alertType) {
        super(alertType);

        var stage = (Stage)getDialogPane().getScene().getWindow();
        stage.getIcons().addAll(LStage.LOGO16, LStage.LOGO32, LStage.LOGO64);
        getDialogPane().getStylesheets().add(getClass().getResource("/com/laeben/corelauncher/style/controls/defcontrols.css").toString());
        getDialogPane().getStylesheets().add(getClass().getResource("/com/laeben/corelauncher/style/controls/alert.css").toString());
    }

    public static CMsgBox msg(AlertType type, String title, String desc){
        return new CMsgBox(type).setInfo(title, desc, false);
    }

    public static CMsgBox msgBean(AlertType type, String title, String desc){
        return new CMsgBox(type).setInfo(title, desc, true);
    }

    public CMsgBox setButtons(ButtonType... types){
        getButtonTypes().clear();
        getButtonTypes().addAll(types);

        return this;
    }

    public CMsgBox setInfo(String title, String desc, boolean isBean){
        setHeaderText(title);
        setTitle(title);
        if (isBean){
            var field = new TextArea();
            field.setText(desc);
            field.setEditable(false);
            getDialogPane().setContent(field);
        }
        else
            setContentText(desc);
        return this;
    }
}
