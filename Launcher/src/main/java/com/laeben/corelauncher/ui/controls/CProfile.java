package com.laeben.corelauncher.ui.controls;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.minecraft.wrappers.Vanilla;
import com.laeben.corelauncher.ui.controller.Mods;
import com.laeben.corelauncher.ui.controller.ProfileEdit;
import com.laeben.corelauncher.ui.controller.Worlds;
import com.laeben.corelauncher.ui.entities.LProfile;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.OSUtils;
import com.laeben.core.entity.Path;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.util.stream.Stream;

public class CProfile extends ListCell<LProfile> {

    private Node gr;
    private LProfile profile;

    public CProfile(){
        var loader = LStage.getDefaultLoader(CoreLauncherFX.class.getResource("/com/laeben/corelauncher/entities/cprofile.fxml"));
        loader.setController(this);
        try{
            setGraphic(gr = loader.load());
        }
        catch (IOException e){
            Logger.getLogger().log(e);
        }
    }

    @FXML
    private ImageView imgProfile;
    @FXML
    private Label lblProfileName;
    @FXML
    private Label lblProfileVersion;
    @FXML
    private Label lblProfileDescription;
    @FXML
    private ContextMenu contextMenu;
    @FXML
    private Button btnMenu;
    @FXML
    private Button btnPlay;
    @FXML
    private MenuItem btnMods;
    @FXML
    private MenuItem btnWorlds;
    @FXML
    private MenuItem btnEdit;
    @FXML
    private MenuItem btnBackup;
    @FXML
    private MenuItem btnSend;
    @FXML
    private MenuItem btnOpenFolder;
    @FXML
    private MenuItem btnDelete;

    @Override
    protected void updateItem(LProfile profile, boolean empty) {
        super.updateItem(profile, empty);

        if (profile == null || empty){
            setGraphic(null);
            return;
        }

        this.profile = profile;

        var p = profile.getProfile();

        imgProfile.setImage(p.getWrapper().getIcon());
        lblProfileName.setText(p.getName());
        lblProfileVersion.setText(p.getVersionId());

        btnPlay.getStyleClass().set(1, profile.selected() ? "profile-sel" : "profile-unsel");
        //btnPlay.setStyle(profile.selected() ? "-fx-background-color: aqua!important;" : "-fx-background-color: #303030!important;");
        btnPlay.setText(profile.selected() ? "▶" : ">");

        btnEdit.setOnAction(a -> ProfileEdit.open(p));

        btnSend.setOnAction(a -> {
            var profileJson = p.getPath().to("profile.json");
            var chooser = new FileChooser();
            chooser.setInitialFileName(p.getName() + ".json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            var file = chooser.showSaveDialog(lblProfileName.getScene().getWindow());
            if (file == null)
                return;
            try{
                profileJson.copy(Path.begin(file.toPath()));
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

        btnBackup.setOnAction(a -> {
            var chooser = new FileChooser();
            chooser.setInitialFileName(p.getName() + ".zip");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
            var file = chooser.showSaveDialog(lblProfileName.getScene().getWindow());
            if (file == null)
                return;
            try{
                Profiler.backup(p, Path.begin(file.toPath()));
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

        var res = p.getResources();
        var ow = p.getOnlineWorlds();
        var lw = p.getLocalWorlds();
        String resInfo = res.size() + Translator.translate("profile.resources") + " / " + (Stream.concat(ow.stream(), lw.stream()).distinct().count()) + Translator.translate("profile.worlds");

        if (!(p.getWrapper() instanceof Vanilla)){
            var mods = p.getMods();
            var packs = p.getModpacks();

            lblProfileDescription.setText(Translator.translateFormat("profile.modState", p.getWrapperVersion(), mods.size(), packs.size(), resInfo));
        }
        else
            lblProfileDescription.setText(resInfo);

        btnDelete.setOnAction((a) ->
                Profiler.getProfiler().deleteProfile(p));


        btnPlay.setOnMouseClicked((a) -> profile.setSelected(true));


        btnMods.setOnAction(a -> Mods.open(p).show());
        btnWorlds.setOnAction(a -> Worlds.open(p).show());

        btnOpenFolder.setOnAction(a -> OSUtils.openFolder(p.getPath().toFile().toPath()));

        setGraphic(gr);
    }

    @FXML
    private void initialize(){
        btnMenu.setOnMouseClicked((a) ->
                contextMenu.show((Button)a.getSource(), a.getScreenX() + 10, a.getScreenY() + 10));

    }

}
