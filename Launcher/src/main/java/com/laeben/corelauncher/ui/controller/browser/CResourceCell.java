package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.minecraft.modding.entity.CResource;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;

import java.util.function.Consumer;

public class CResourceCell extends ListCell<CResource> {

    private final Node node;
    private Consumer<CResourceCell> onDelete;

    public CResourceCell(){
        node = UI.getUI().load(CoreLauncherFX.class.getResource("layout/cells/cresource.fxml"), this);
    }

    @FXML
    public CView icon;
    @FXML
    public Label lblName;
    @FXML
    public Label lblAuthor;
    @FXML
    public TextArea txtDesc;
    @FXML
    public CButton btnDelete;
    
    public CResourceCell setOnDelete(Consumer<CResourceCell> onDelete){
        this.onDelete = onDelete;

        return this;
    }

    @Override
    protected void updateItem(CResource item, boolean empty) {
        if (empty || item == null){
            setGraphic(null);
            return;
        }

        icon.setImageAsync(item.getIcon());
        lblName.setText(item.name);
        lblAuthor.setText(item.authors == null ? null : String.join(",", item.authors));
        txtDesc.setText(item.desc);
        btnDelete.setOnMouseClicked(a -> {
            if (onDelete != null)
                onDelete.accept(this);
        });

        super.updateItem(item, false);
        setGraphic(node);
    }
}
