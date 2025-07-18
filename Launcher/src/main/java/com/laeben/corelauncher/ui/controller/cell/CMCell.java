package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.util.DateUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Predicate;

// Mod Cell
public class CMCell extends CCell<CResource>{

    public static final PseudoClass INSTALLED =  PseudoClass.getPseudoClass("installed");

    private CResource item;

    private final CButton btnInstall;

    private final BooleanProperty installed;
    private Consumer<CMCell> clicked;
    private Predicate<CMCell> exists;

    public CMCell() {
        super("layout/cells/ccell.fxml");
        var res = CoreLauncherFX.class.getResource("style/cells/cmcell.css");
        assert res != null;
        getStylesheets().add(res.toExternalForm());

        btnInstall = new CButton();
        btnInstall.setId("btnInstall");
        btnInstall.setText("+");
        btnInstall.enableTransparentAnimation();

        installed = new SimpleBooleanProperty();
        installed.addListener((a, b, isInstalled) -> {
            btnInstall.setText(isInstalled ? "—" : "+");
            btnInstall.pseudoClassStateChanged(INSTALLED, isInstalled);
        });

        box.getChildren().add(btnInstall);
    }

    @FXML
    private CView image;
    @FXML
    private Label lblName;
    @FXML
    private HBox box;

    public CMCell setOnInstallClicked(Consumer<CMCell> clicked){
        this.clicked = clicked;
        return this;
    }

    public CMCell setOnTestExistance(Predicate<CMCell> exists){
        this.exists = exists;
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

        /*if (preferences.getProfile() != null){
            var res = preferences.getProfile().getResource(item.id);
            setInstalled(res != null && res.fileName != null && res.fileName.equals(item.fileName));
        }
        else
            setInstalled(false);*/

        if (exists != null)
            setInstalled(exists.test(this));
        else
            setInstalled(false);


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
