package com.laeben.corelauncher.ui.controller.browser.cell;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.util.entity.LogType;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class CResourceCell extends ListCell<CResource> {

    private final Node node;
    private Consumer<CResource> onDelete;

    public CResourceCell(){
        node = UI.getUI().load(CoreLauncherFX.class.getResource("layout/cells/cresource.fxml"), this);
        setPrefWidth(0);

        header.setOnMouseClicked(a -> {
            if (getItem() == null || getItem().resourceUrl == null)
                return;

            try {
                OSUtil.openURL(getItem().resourceUrl);
            } catch (Exception e) {
                Logger.getLogger().log(LogType.WARN, "Unable to open URL: " + getItem().resourceUrl);
                Logger.getLogger().log(e);
            }
        });

        btnDelete.setOnMouseClicked(a -> {
            if (onDelete != null)
                onDelete.accept(getItem());
        });
    }

    @FXML
    private CView icon;
    @FXML
    private HBox header;
    @FXML
    private Label lblName;
    @FXML
    private Label lblAuthor;
    @FXML
    private Text txtDesc;
    @FXML
    private CButton btnDelete;
    
    public CResourceCell setOnDelete(Consumer<CResource> onDelete){
        this.onDelete = onDelete;

        return this;
    }

    @Override
    protected void updateItem(CResource item, boolean empty) {
        super.updateItem(item, false);

        if (empty || item == null){
            setGraphic(null);
            return;
        }

        icon.setCornerRadius(72, 72, 16);
        icon.setImageAsync(item.getIcon());
        lblName.setText(item.name);

        lblAuthor.setText(item.authors == null ? null : String.join(",", item.authors));
        txtDesc.setText(item.desc);

        setGraphic(node);
    }
}
