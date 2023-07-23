package com.laeben.corelauncher.ui.controls;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.File;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;
import com.laeben.corelauncher.ui.entities.LModLink;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;

public class BMod extends ListCell<LModLink> {
    private final Node gr;
    private LModLink link;
    private CResource exists;

    private final ContextMenu menu;
    private List<File> allFiles;


    public BMod(){
        var path = CoreLauncherFX.class.getResource("/com/laeben/corelauncher/entities/bmod.fxml");
        setGraphic(gr = FXManager.getManager().open(this, path));

        menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #252525;");
    }

    @FXML
    private ImageView img;
    @FXML
    private Label lblName;
    @FXML
    private Label lblAuthor;
    @FXML
    private TextField txtDesc;
    @FXML
    private CButton btnInstall;
    @FXML
    private CButton btnMore;

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

        if (i.logo != null)
            img.setImage(new Image(i.logo.thumbnailUrl, true));

        lblName.setText(i.name);
        lblAuthor.setText(i.authors == null ? "" : String.join(",", i.authors.stream().map(x -> x.name).toList()));
        txtDesc.setText(i.summary);

        exists = profile.getResource(i.id);

        btnInstall.setText(exists != null ? "-" : "⭳");

        setGraphic(gr);
    }

    @FXML
    private void initialize(){

        btnInstall.setOnMouseClicked(a -> new Thread(() -> {
            var profile = link.profile();

            if (exists == null){
                var resource = CurseForge.getForge().getFullResource(profile.getVersionId(), profile.getWrapper().getType(), link.resource());
                var mod = CResource.fromResourceGeneric(profile.getVersionId(), profile.getWrapper().getIdentifier(), resource);
                Platform.runLater(() -> FXManager.getManager().focus("main"));
                try{
                    CurseForge.getForge().include(profile, mod);
                    Platform.runLater(() -> {
                        btnInstall.setText("-");
                        FXManager.getManager().focus("forgebrowser");
                    });

                    exists = mod;
                } catch (NoConnectionException | HttpException e) {
                    exists = null;
                }
            }
            else{
                CurseForge.getForge().remove(profile, CResource.fromResourceGeneric(profile.getVersionId(), profile.getWrapper().getIdentifier(), link.resource()));
                Platform.runLater(() -> btnInstall.setText("⭳"));
                exists = null;
            }
        }).start());

        btnMore.setOnMouseClicked(a -> {
            menu.getItems().clear();
            var profile = link.profile();

            if (allFiles == null || allFiles.size() == 0){
                var all = CurseForge.getForge().getFullResource(profile.getVersionId(), profile.getWrapper().getType(), link.resource());
                allFiles = all.latestFiles;
            }

            for(var file : allFiles){
                var item = new MenuItem();
                item.setStyle("-fx-text-fill: white;");

                item.setText(file.fileName);
                item.setOnAction(b -> {

                    if (exists != null){
                        CurseForge.getForge().remove(profile, exists);
                    }

                    var mod = CResource.fromResourceGeneric(profile.getVersionId(), profile.getWrapper().getIdentifier(), link.resource());
                    mod.setFile(file);

                    try{
                        CurseForge.getForge().include(profile, mod);

                        exists = mod;
                        btnInstall.setText("-");
                    } catch (NoConnectionException | HttpException e) {
                        exists = null;
                    }
                });

                menu.getItems().add(item);
            }

            menu.show(btnMore, a.getScreenX(), a.getScreenY());
        });
    }
}
