package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;
import com.laeben.corelauncher.minecraft.modding.entities.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.DependencyInfo;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.Version;
import com.laeben.corelauncher.ui.controls.CMsgBox;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.util.List;

public class MBMod extends BMod {

    private List<Version> versions;

    @Override
    public void onUpdate(Object resource, Profile profile) {
        var i = (ResourceRinth)resource;

        if (i.icon != null && !i.icon.isBlank())
            img.setImage(new Image(i.icon, true));

        lblName.setText(i.title);
        lblAuthor.setText(i.author);
        txtDesc.setText(i.description);

        exists = profile.getResource(i.getId());

        btnInstall.setText(exists != null ? "-" : "⭳");
    }

    @FXML
    private void initialize(){

        btnInstall.setOnMouseClicked(a -> new Thread(() -> {
            var profile = link.profile();
            var res = (ResourceRinth)link.resource();

            if (exists == null){
                refresh();

                if (versions.isEmpty()){
                    Platform.runLater(() -> CMsgBox.msg(Alert.AlertType.WARNING, Translator.translate("error.oops"), Translator.translate("mods.error.noVersions")).show());
                    return;
                }

                inc(versions.get(0), profile, res);
            }
            else{
                Modder.getModder().remove(profile, exists);
                Platform.runLater(() -> btnInstall.setText("⭳"));
                exists = null;
            }
        }).start());

        btnMore.setOnMouseClicked(a -> {
            refresh();

            var profile = link.profile();
            var res = (ResourceRinth)link.resource();

            menu.getItems().clear();
            menu.getItems().addAll(versions.stream().map(b -> {
                var item = new MenuItem();
                item.setStyle("-fx-text-fill: white;");

                item.setText(b.name);
                item.setOnAction(c -> new Thread(() -> {
                    if (exists != null)
                        Modder.getModder().remove(profile, exists);

                    inc(b, profile, res);
                }).start());

                return item;
            }).toList());

            menu.show(btnMore, a.getScreenX(), a.getScreenY());
        });
    }

    private void inc(Version version, Profile profile, ResourceRinth res){
        if (res.projectType.equals(ResourceType.MODPACK.getName()))
            Platform.runLater(() -> FXManager.getManager().focus("main"));

        try{
            include(version.id, profile, res);

            Platform.runLater(() -> {
                btnInstall.setText("-");
                FXManager.getManager().focus("modbrowser");
            });
        } catch (NoConnectionException | HttpException ignored) {

        }
    }

    public void include(String versionId, Profile profile, ResourceRinth res) throws NoConnectionException, HttpException {
        try {
            var versions = Modrinth.getModrinth().getVersions(List.of(versionId), DependencyInfo.includeDependencies(profile.getVersionId(), profile.getWrapper().getIdentifier()));
            var resources = Modrinth.getModrinth().getResources(versions.stream().map(x -> x.projectId).toList());
            var all = versions.stream().map(x -> (CResource)CResource.fromRinthResourceGeneric(resources.stream().filter(s -> s.getId().equals(x.projectId)).findFirst().get(), x)).toList();

            Modder.getModder().includeAll(profile, all);

            exists = all.stream().filter(x -> x.id.equals(res.getId())).findFirst().get();
        } catch (NoConnectionException | HttpException e) {
            exists = null;
            throw e;
        }
    }

    public void refresh(){
        if (versions != null && !versions.isEmpty())
            return;

        var profile = link.profile();
        var res = (ResourceRinth)link.resource();

        try{
            versions = Modrinth.getModrinth().getProjectVersions(res.getId(), profile.getVersionId(), ResourceType.isGlobal(res.projectType) ? null : profile.getWrapper().getIdentifier(), DependencyInfo.noDependencies());
        } catch (NoConnectionException | HttpException e) {
            versions = List.of();
        }
    }
}
