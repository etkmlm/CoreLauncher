package com.laeben.corelauncher.ui.controls;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.Resource;
import com.laeben.corelauncher.minecraft.modding.entities.Mod;
import com.laeben.corelauncher.ui.entities.LMod;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;

public class CMod extends ListCell<LMod> {
    private final Node gr;
    private Resource resource;

    public CMod(){
        var path = CoreLauncherFX.class.getResource("/com/laeben/corelauncher/entities/cmod.fxml");
        setGraphic(gr = FXManager.getManager().open(this, path));
    }

    @FXML
    public ImageView img;
    @FXML
    public Label lblName;
    @FXML
    public Label lblFileName;
    @FXML
    public Label lblPack;
    @FXML
    public Button btnRemove;

    @Override
    protected void updateItem(LMod i, boolean empty) {
        super.updateItem(i, empty);

        if (empty || i == null){
            setGraphic(null);
            return;
        }

        var item = i.get();

        img.setImage(item.getIcon());

        if (item instanceof Mod m && m.mpId != 0){
            var mp = i.getProfile().getModpacks().stream().filter(x -> x.id == m.mpId).findFirst();
            mp.ifPresent(x -> lblPack.setText(x.name));
        }
        else
            lblPack.setText("");

        lblFileName.setText(item.fileName);
        lblName.setText(item.name);
        btnRemove.setOnMouseClicked(a -> i.onAction().accept(i));

        setGraphic(gr);
    }
}
