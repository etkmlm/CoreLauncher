package com.cdev.corelauncher.ui.controls;

import com.cdev.corelauncher.CoreLauncherFX;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.ui.controller.ProfileEdit;
import com.cdev.corelauncher.ui.entities.LProfile;
import com.cdev.corelauncher.ui.entities.LStage;
import com.cdev.corelauncher.utils.OSUtils;
import com.cdev.corelauncher.utils.entities.Path;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.IOException;

public class CProfile extends ListCell<LProfile> {

    private final Node gr;
    private LProfile profile;

    public CProfile(){
        var loader = LStage.getDefaultLoader(CoreLauncherFX.class.getResource("/com/cdev/corelauncher/entities/cprofile.fxml"));
        loader.setController(this);
        try{
            setGraphic(gr = loader.load());
        }
        catch (IOException e){
            throw new RuntimeException(e);
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

        imgProfile.setImage(profile.getProfile().getWrapper().getIcon());
        lblProfileName.setText(profile.getProfile().getName());
        lblProfileVersion.setText(profile.getProfile().getVersionId());

        btnPlay.getStyleClass().set(1, profile.selected() ? "profile-sel" : "profile-unsel");
        //btnPlay.setStyle(profile.selected() ? "-fx-background-color: aqua!important;" : "-fx-background-color: #303030!important;");
        btnPlay.setText(profile.selected() ? "▶" : ">");

        btnEdit.setOnAction(a -> ProfileEdit.open(profile.getProfile()));

        btnSend.setOnAction(a -> {
            var profileJson = profile.getProfile().getPath().to("profile.json");
            var chooser = new FileChooser();
            chooser.setInitialFileName(profile.getProfile().getName() + ".json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            var file = chooser.showSaveDialog(lblProfileName.getScene().getWindow());
            if (file == null)
                return;
            profileJson.copy(Path.begin(file.toPath()));
        });

        btnBackup.setOnAction(a -> {
            var chooser = new FileChooser();
            chooser.setInitialFileName(profile.getProfile().getName() + ".zip");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
            var file = chooser.showSaveDialog(lblProfileName.getScene().getWindow());
            if (file == null)
                return;
            Profiler.backup(profile.getProfile(), Path.begin(file.toPath()));
        });

        /*if (profile.getMods() != null)
            lblProfileDescription.setText(profile.getMods().size() + " mods");*/


        btnOpenFolder.setOnAction(a -> OSUtils.openFolder(profile.getProfile().getPath().toFile().toPath()));

        setGraphic(gr);
    }

    @FXML
    private void initialize(){
        btnMenu.setOnMouseClicked((a) ->
                contextMenu.show((Button)a.getSource(), a.getScreenX() + 10, a.getScreenY() + 10));

        /*btnOpenFolder.setOnMouseClicked((a) ->
                Desktop.getDesktop().browseFileDirectory(profile.getPath().toFile()));*/

        btnDelete.setOnAction((a) ->
                Profiler.getProfiler().deleteProfile(profile.getProfile()));


        btnPlay.setOnMouseClicked((a) -> profile.setSelected(true));

    }

}
