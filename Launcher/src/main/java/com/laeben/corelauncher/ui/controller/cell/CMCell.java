package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.corelauncher.api.util.DateUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.CResource;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Predicate;

// Mod Cell
public class CMCell extends CCell<CResource>{

    private CResource item;

    private final CButton btnInstall;
    private Profile profile;

    private final BooleanProperty installed;
    private Consumer<CMCell> clicked;

    public CMCell() {
        super("layout/cells/ccell.fxml");

        btnInstall = new CButton();
        btnInstall.setText("⭳");
        btnInstall.setStyle("-fx-font-size: 14pt;-fx-background-color: transparent;-fx-min-width: 45px;-fx-pref-width: 45px;");
        btnInstall.enableTransparentAnimation();

        installed = new SimpleBooleanProperty();
        installed.addListener(a -> btnInstall.setText(isInstalled() ? "—" : "⭳"));

        box.getChildren().add(btnInstall);
    }

    @FXML
    private CView image;
    @FXML
    private Label lblName;
    @FXML
    private HBox box;


    public CMCell setProfile(Profile profile) {
        this.profile = profile;
        return this;
    }

    public CMCell setOnInstallClicked(Consumer<CMCell> clicked){
        this.clicked = clicked;
        return this;
    }


    @Override
    public CCell setItem(CResource item) {
        this.item = item;

        node.setOnMouseEntered(a -> fade.playFromStart());
        node.setOnMouseExited(a -> {
            fade.play();
            fade.jumpTo(Duration.ZERO);
            fade.stop();
        });

        //image.setImage(item.getIcon());
        image.setManaged(false);
        image.setVisible(false);
        var text = item.fileName + " - " + DateUtil.toString(item.fileDate, Configurator.getConfig().getLanguage());

        lblName.setText(text);
        lblName.setTooltip(new Tooltip(text));

        var res = profile.getResource(item.id);
        setInstalled(res != null && res.fileName != null && res.fileName.equals(item.fileName));

        btnInstall.setOnMouseClicked(a -> {
            if (clicked != null)
                clicked.accept(this);
        });

        super.getChildren().clear();
        super.getChildren().add(node);

        return this;
    }

    public void setInstalled(boolean i){
        installed.set(i);
    }

    public boolean isInstalled() {
        return installed.get();
    }

    @Override
    public CResource getItem() {
        return item;
    }
}
