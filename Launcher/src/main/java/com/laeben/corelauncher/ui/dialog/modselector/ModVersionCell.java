package com.laeben.corelauncher.ui.dialog.modselector;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.util.DateUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.ui.control.CButton;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class ModVersionCell extends ListCell<CResource> {
    private final HBox root;
    private final Label label;
    private final CButton btnInstall;

    private final BooleanProperty installed;
    private Consumer<CResource> clicked;

    public ModVersionCell() {
        var res = CoreLauncherFX.class.getResource("style/cells/modversion.css");
        assert res != null;
        getStylesheets().add(res.toExternalForm());

        root = new HBox();
        root.setAlignment(Pos.CENTER_LEFT);
        root.setSpacing(16);
        root.setPrefWidth(0);

        label = new Label();
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);

        btnInstall = new CButton();
        btnInstall.setId("btnInstall");
        btnInstall.setText("+");
        btnInstall.setOnMouseClicked(a -> {
            if (clicked != null)
                clicked.accept(getItem());
        });

        installed = new SimpleBooleanProperty();
        installed.addListener((a, b, isInstalled) -> {
            btnInstall.setText(isInstalled ? "—" : "+");
            btnInstall.setStyle(isInstalled ? null : "-fx-font-size: 16pt;");
        });

        root.getChildren().setAll(label, btnInstall);
    }

    public ModVersionCell setOnInstallClicked(Consumer<CResource> clicked){
        this.clicked = clicked;
        return this;
    }

    public ModVersionCell setInstalledProperty(ObjectProperty<CResource> property){
        if (installed.isBound()) installed.unbind();

        installed.bind(Bindings.createBooleanBinding(() -> getItem() != null && getItem().equals(property.get()), property, itemProperty()));
        return this;
    }

    @Override
    public void updateItem(CResource item, boolean empty){
        super.updateItem(item, empty);

        if (item == null || empty){
            setGraphic(null);
            return;
        }

        var text = item.fileName + " - " + DateUtil.toString(item.fileDate, Configurator.getConfig().getLanguage());

        label.setText(text);
        label.setTooltip(new Tooltip(text));

        setGraphic(root);
    }
}
