package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;
import com.laeben.corelauncher.minecraft.modding.entities.Modpack;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.util.List;

public class FBMod extends BMod {
    private List<ForgeFile> allFiles;

    @Override
    public void onUpdate(Object resource, Profile profile) {

        var i = (ResourceForge)resource;

        if (i.logo != null)
            img.setImage(new Image(i.logo.thumbnailUrl, true));

        lblName.setText(i.name);
        lblAuthor.setText(i.authors == null ? "" : String.join(",", i.authors.stream().map(x -> x.name).toList()));
        txtDesc.setText(i.summary);
        exists = profile.getResource(i.id);

        btnInstall.setText(exists != null ? "-" : "⭳");
    }

    @FXML
    private void initialize(){

        btnInstall.setOnMouseClicked(a -> new Thread(() -> {
            var profile = link.profile();
            var res = (ResourceForge)link.resource();

            if (exists == null){
                ResourceForge resource = null;
                try {
                    resource = CurseForge.getForge().getFullResource(profile.getVersionId(), profile.getWrapper().getType(), res);
                } catch (NoConnectionException | HttpException ignored) {

                }

                if (resource == null)
                    return;

                var mod = CResource.fromForgeResourceGeneric(profile.getVersionId(), profile.getWrapper().getIdentifier(), resource);

                List<CResource> all = null;

                try{
                    all = CurseForge.getForge().getDependencies(List.of(mod), profile);
                } catch (NoConnectionException ignored) {

                }

                if (all == null)
                    all = List.of(mod);

                if (mod instanceof Modpack)
                    Platform.runLater(() -> FXManager.getManager().focus("main"));
                try{
                    Modder.getModder().includeAll(profile, all);
                    Platform.runLater(() -> {
                        btnInstall.setText("-");
                        FXManager.getManager().focus("modbrowser");
                    });

                    exists = mod;
                } catch (NoConnectionException | HttpException e) {
                    exists = null;
                }
            }
            else{
                Modder.getModder().remove(profile, exists);
                Platform.runLater(() -> btnInstall.setText("⭳"));
                exists = null;
            }
        }).start());

        btnMore.setOnMouseClicked(a -> {
            menu.getItems().clear();
            var profile = link.profile();
            var res = (ResourceForge)link.resource();

            if (allFiles == null || allFiles.isEmpty()){
                ResourceForge all = null;
                try {
                    all = CurseForge.getForge().getFullResource(profile.getVersionId(), profile.getWrapper().getType(), res);
                } catch (NoConnectionException | HttpException ignored) {

                }
                if (all != null)
                    allFiles = all.latestFiles;
            }

            for(var file : allFiles){
                var item = new MenuItem();
                item.setStyle("-fx-text-fill: white;");

                item.setText(file.fileName);
                item.setOnAction(b -> {

                    if (exists != null){
                        Modder.getModder().remove(profile, exists);
                    }


                    var mod = CResource.fromForgeResourceGeneric(profile.getVersionId(), profile.getWrapper().getIdentifier(), res);
                    mod.setFile(CResource.fromForgeFile(file, (int)mod.id));

                    if (mod instanceof Modpack){
                        FXManager.getManager().focus("main");
                    }

                    try{
                        var all = CurseForge.getForge().getDependencies(List.of(mod), profile);

                        Modder.getModder().includeAll(profile, all);

                        exists = mod;
                        btnInstall.setText("-");
                        FXManager.getManager().focus("modbrowser");
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
