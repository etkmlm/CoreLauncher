package com.laeben.corelauncher.ui.dialog;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.entity.ImageEntity;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class DImageSelector extends CDialog<ImageEntity> {

    public DImageSelector(){
        super("layout/dialog/imageselector.fxml", false);

        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
    }

    @FXML
    private CButton btnClose;
    @FXML
    private FlowPane pnImages;
    @FXML
    private CView image;
    @FXML
    private Rectangle rectImage;
    @FXML
    private TextField txtInput;
    @FXML
    private CButton btnSelect;
    @FXML
    private CButton btnDone;
    @FXML
    private CheckBox chkOffline;

    private boolean network = false;
    private boolean embedded = false;
    private boolean ok = false;
    private String url;
    private Path path;

    private CView getView(String url){
        var img = new CView();
        img.setFitWidth(48);
        img.setFitHeight(48);
        img.setCornerRadius(48, 48, 8);
        img.setImage(CoreLauncherFX.getLocalImage(url));
        img.setOnMouseClicked(a -> {
            network = false;
            path = null;
            embedded = true;
            ok = true;
            this.url = url;
            image.setImage(CoreLauncherFX.getLocalImage(url));
        });

        return img;
    }
    public void initialize(){
        image.setCornerRadius(image.getFitWidth(), image.getFitHeight(), 16);
        rectImage.setFill(new ImagePattern(CoreLauncherFX.getDefaultImage(128)));



        pnImages.getChildren().addAll(
                getView("creeper.png"),
                getView("zombie.png"),
                getView("skeleton.png"),
                getView("wither.png")
        );

        btnClose.setOnMouseClicked(a -> {
            setResult(null);
            close();
        });
        btnDone.setOnMouseClicked(a -> {
            if (!ok){
                setResult(ImageEntity.empty());
                return;
            }

            if (url != null) {
                try {
                    if (embedded)
                        setResult(ImageEntity.fromEmbedded(url));
                    else
                        setResult(chkOffline.isSelected() ? NetUtil.downloadImage(Configurator.getConfig().getImagePath(), url) : ImageEntity.fromNetwork(url));
                } catch (MalformedURLException e) {
                    setResult(null);
                }
            }
            else{
                path.copy(Configurator.getConfig().getImagePath().to(path.getName()));
                setResult(ImageEntity.fromLocal(path.getName()));
            }

            close();
        });
        txtInput.setOnKeyPressed(a -> {
            if (!network){
                btnSelect.setText(Translator.translate("option.apply"));
                network = true;
            }
        });
        btnSelect.setOnMouseClicked(a -> {
            if (network){
                url = txtInput.getText();
                try {
                    image.setImage(new URL(url));
                    rectImage.setVisible(false);
                    ok = true;
                } catch (MalformedURLException e) {
                    url = null;
                    ok = false;
                }

                network = false;
                embedded = false;
                btnSelect.setText(Translator.translate("option.select"));
            }
            else{
                ok = false;
                embedded = false;
                var dialog = new FileChooser();
                dialog.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("PNG,JPG", "*.png", "*.jpg", "*.jpeg"));
                var file = dialog.showOpenDialog(getOwner());
                if (file == null || !file.exists())
                    return;
                path = Path.begin(file.toPath());
                try(var m = new FileInputStream(file)) {
                    image.setImage(new Image(m));
                    rectImage.setVisible(false);
                    ok = true;
                    url = null;
                } catch (IOException e) {
                    path = null;
                }
            }
        });
    }

    public Optional<ImageEntity> action(){
        return super.action();
    }
}
