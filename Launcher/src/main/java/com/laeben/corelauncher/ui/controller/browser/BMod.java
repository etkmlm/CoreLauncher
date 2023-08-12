package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;
import com.laeben.corelauncher.ui.controls.CButton;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public abstract class BMod extends ListCell<LModLink> {
    private final Node gr;
    protected LModLink link;
    protected CResource exists;

    protected final ContextMenu menu;

    public BMod(){
        setGraphic(gr = FXManager.getManager().applyControl(this, "bmod"));

        menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #252525;");
    }

    @FXML
    public ImageView img;
    @FXML
    public Label lblName;
    @FXML
    public Label lblAuthor;
    @FXML
    public TextField txtDesc;
    @FXML
    public CButton btnInstall;
    @FXML
    public CButton btnMore;

    @Override
    protected void updateItem(LModLink li, boolean empty) {
        super.updateItem(li, empty);

        if (empty || li == null){
            setGraphic(null);
            link = null;
            return;
        }

        link = li;

        var i = li.resource();
        var profile = li.profile();

        onUpdate(i, profile);

        setGraphic(gr);
    }

    public abstract void onUpdate(Object resource, Profile profile);
}
