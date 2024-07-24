package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.dialog.CDialog;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CMsgBox extends CDialog<CMsgBox.Result> {

    public enum ResultType {
        YES, NO, OK, CANCEL, OPTION
    }

    public record Result(ResultType result, Object extra){

    }

    private final List<ResultType> resultTypes;

    public CMsgBox(Alert.AlertType type) {
        super("layout/dialog/messagebox.fxml", false);

        var n = (Region)node;
        getDialogPane().prefHeightProperty().bind(n.heightProperty());
        getDialogPane().prefWidthProperty().bind(n.widthProperty());

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        getDialogPane().lookupButton(ButtonType.CANCEL).setStyle("-fx-opacity: 0");

        icon.setImage(CoreLauncherFX.getLocalImage("dialog/dialog-" + type.name().toLowerCase(Locale.US) + ".png"));

        resultTypes = new ArrayList<>();
    }

    public static CMsgBox msg(Alert.AlertType type, String title, String desc){
        return new CMsgBox(type).setInfo(title, desc);
    }

    public CMsgBox setButtons(ResultType... types){
        resultTypes.clear();
        resultTypes.addAll(List.of(types));
        return this;
    }

    public CMsgBox setInfo(String title, String desc){
        setTitle(title);

        this.title.setText(title);
        this.content.setText(desc);
        return this;
    }

    @FXML
    private Label title;
    @FXML
    private TextArea content;
    @FXML
    private CView icon;
    @FXML
    private HBox buttons;
    @FXML
    private CButton btnClose;

    @FXML
    public void initialize(){
        icon.setCornerRadius(48, 48, 16);

        content.maxWidthProperty().bind(title.widthProperty().multiply(1.75));

        content.textProperty().addListener(a -> {
            var txt = new Text(content.getText());
            txt.setFont(content.getFont());
            txt.setBoundsType(TextBoundsType.VISUAL);
            txt.setWrappingWidth(content.getWidth());

            content.setPrefHeight(txt.getLayoutBounds().getHeight()*2.5);
        });

        btnClose.enableTransparentAnimation();
        btnClose.setOnMouseClicked(a -> {
            setResult(null);
            close();
        });
    }

    private void setButtons(){
        buttons.getChildren().clear();
        int optCount = 1;
        for (var x : resultTypes){
            var btn = new CButton();
            btn.getStyleClass().add("message-box-button");
            int fOptCount = optCount;
            btn.setOnMouseClicked(a -> {
                setResult(new Result(x, fOptCount));
                close();
            });
            if (x == ResultType.OPTION)
                btn.setText(Translator.translateFormat("option.option", optCount++));
            else
                btn.setText(Translator.translate("option." + x.name().toLowerCase(Locale.US)));
            buttons.getChildren().add(btn);
        }
    }

    public Optional<Result> executeForResult(){
        setButtons();
        return showAndWait();
    }

    public void execute(){
        show();
    }
}
