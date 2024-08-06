package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CField;
import com.laeben.corelauncher.util.JavaManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class CJava extends CCell<Java> {

    private Java java;
    private final BooleanProperty editMode;

    public CJava() {
        super("layout/cells/cjava.fxml");

        editMode = new SimpleBooleanProperty();
        editMode.addListener(a -> {
            if (editMode.get()){
                txtName.setEditable(true);
                txtName.setCursor(Cursor.TEXT);
            }
            else{
                txtName.setEditable(false);
                txtName.setCursor(Cursor.DEFAULT);
                node.requestFocus();
                if (JavaManager.getManager().renameCustomJava(java, txtName.getText()))
                    txtName.setText(java.getName());
            }
        });
    }

    @FXML
    private Label lblVersion;

    @FXML
    private CField txtName;
    @FXML
    private TextField txtPath;
    @FXML
    private CButton btnAction;

    @Override
    public CJava setItem(Java item) {
        this.java = item;

        txtName.setEditable(false);
        txtName.setCursor(Cursor.DEFAULT);

        if (java == null || java.isEmpty()){
            super.getChildren().clear();
            return this;
        }

        btnAction.enableTransparentAnimation();

        txtPath.setCursor(Cursor.HAND);

        txtName.focusedProperty().addListener(a -> {
            if (txtName.isFocused() && !editMode.get())
                editMode.set(true);

            if (!txtName.isFocused() && editMode.get())
                editMode.set(false);
        });
        txtName.setOnKeyPressed(a -> {
            if (a.getCode() == KeyCode.ENTER)
                editMode.set(false);
        });
        txtName.setFocusedAnimation(Color.TEAL, Duration.millis(200));
        txtName.setText(java.getName());

        txtPath.setOnMouseClicked(a -> OSUtil.openFolder(java.getPath().toFile().toPath()));
        txtPath.setText(java.getPath().toString());
        btnAction.setOnMouseClicked((a) -> JavaManager.getManager().deleteJava(java));

        String cls;
        if (java.majorVersion >= 16)
            cls = "red";
        else if (java.majorVersion >= 11)
            cls = "orange";
        else
            cls = "green";

        lblVersion.getStyleClass().setAll("label", cls);
        lblVersion.setText(String.valueOf(java.majorVersion));

        btnAction.setText("—");

        super.getChildren().clear();
        super.getChildren().add(node);
        return this;
    }

    @Override
    public Java getItem() {
        return java;
    }
}
