package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.wrap.ExtensionWrapper;
import com.laeben.corelauncher.wrap.entity.Extension;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

public class CECell extends CCell<Extension>{

    private Extension item;

    public CECell() {
        super("layout/cells/extcell.fxml");
    }

    @FXML
    private Label lblName;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblAuthor;
    @FXML
    private Label lblVersion;

    @FXML
    private CButton btnRemove;
    @FXML
    private CButton btnOpenFolder;

    @FXML
    private CView icon;
    @FXML
    private ImageView warning;


    @Override
    public CCell setItem(Extension item) {
        this.item = item;

        lblName.setText(item.getName());
        lblDescription.setText(item.getDescription());
        lblVersion.setText(item.getVersion());
        lblAuthor.setText(item.getAuthor());
        warning.setVisible(!item.isCompatible());
        Tooltip.install(warning, new Tooltip(Translator.translateFormat("extensions.incompatible", item.getTarget())));

        icon.setImage(item.getIcon() != null ? item.getIcon() : ImageUtil.getDefaultImage(128));

        btnRemove.setOnMouseClicked(a -> ExtensionWrapper.getWrapper().removeExtension(item));

        super.getChildren().clear();
        super.getChildren().add(node);
        return this;
    }

    @Override
    public Extension getItem() {
        return item;
    }

    @FXML
    public void initialize(){
        icon.setCornerRadius(128, 128, 10);
        btnRemove.enableTransparentAnimation();
    }


}
