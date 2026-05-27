package com.laeben.corelauncher.ui.dialog;

import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CColorPicker;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.Optional;

public class DColorPicker extends CDialog<Color> {
    public DColorPicker(Window owner){
        super("layout/dialog/colorpicker.fxml", true, owner);

        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
    }

    public DColorPicker(Color color, Window owner){
        this(owner);

        picker.setSelectedColor(color);
    }

    @FXML
    private CColorPicker picker;
    @FXML
    private TextField txtColor;
    @FXML
    private Pane pColor;
    @FXML
    private CButton btnClose;
    @FXML
    private CButton btnDone;

    @FXML
    private Slider sldOpacity;

    private boolean manualChange = false;

    public void initialize(){
        picker.selectedColorProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            if (!manualChange) {
                int r = (int)Math.round(newValue.getRed() * 255.0);
                int g = (int)Math.round(newValue.getGreen() * 255.0);
                int b = (int)Math.round(newValue.getBlue() * 255.0);
                int a = (int)Math.round(newValue.getOpacity() * 255.0);
                txtColor.setText(a == 255 ? String.format("#%02x%02x%02x", r, g, b) :  String.format("#%02x%02x%02x%02x", r, g, b, a));
            }
            pColor.setBackground(new Background(new BackgroundFill(
                    newValue,
                    new CornerRadii(16),
                    null
            )));
        });
        picker.alphaProperty().bindBidirectional(sldOpacity.valueProperty());
        txtColor.textProperty().addListener((observable, oldValue, newValue) -> {
            try{
                manualChange = true;
                picker.setSelectedColor(Color.web(newValue));
                manualChange = false;
            }
            catch (Exception ignored){}
        });

        btnClose.enableTransparentAnimation();
        btnClose.setOnMouseClicked(a -> {
            setResult(null);
            close();
        });

        btnDone.setOnMouseClicked(a -> {
            setResult(picker.getSelectedColor());
            close();
        });
    }

    public Optional<Color> pickColor(){
        return super.action();
    }
}
