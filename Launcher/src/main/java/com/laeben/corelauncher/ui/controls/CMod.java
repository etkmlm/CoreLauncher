package com.laeben.corelauncher.ui.controls;

import com.laeben.corelauncher.minecraft.modding.entities.Mod;
import com.laeben.corelauncher.minecraft.modding.entities.Modpack;
import com.laeben.corelauncher.minecraft.modding.entities.ModpackContent;
import com.laeben.corelauncher.minecraft.modding.entities.Resourcepack;
import com.laeben.corelauncher.ui.entities.LMod;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;

import java.lang.reflect.Method;
import java.util.Optional;

public class CMod extends ListCell<LMod> {
    private final Node gr;

    public CMod(){
        setGraphic(gr = FXManager.getManager().applyControl(this, "cmod"));
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

        Optional<Modpack> mp = Optional.empty();
        if (item instanceof ModpackContent mpc && mpc.getModpackId() != null)
            mp = i.getProfile().getModpacks().stream().filter(mpc::belongs).findFirst();
        else
            lblPack.setText("");

        mp.ifPresent(x -> lblPack.setText(x.name));

        lblFileName.setText(item.fileName);
        lblName.setText(item.name);
        btnRemove.setOnMouseClicked(a -> i.onAction().accept(i));

        setGraphic(gr);
    }
}
