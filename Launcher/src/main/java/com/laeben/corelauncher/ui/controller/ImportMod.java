package com.laeben.corelauncher.ui.controller;

import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;
import com.laeben.corelauncher.minecraft.modding.entities.Mod;
import com.laeben.corelauncher.minecraft.modding.entities.Resourcepack;
import com.laeben.corelauncher.minecraft.modding.entities.World;
import com.laeben.corelauncher.ui.controls.CButton;
import com.laeben.corelauncher.ui.controls.CMsgBox;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class ImportMod {
    private Profile profile;

    public static LStage open(Profile profile){
        var stage = FXManager.getManager().applyStage("importmod");
        var mod = (ImportMod)stage.getLScene().getController();
        mod.profile = profile;

        return stage;
    }

    @FXML
    public TextField txtName;
    @FXML
    public TextField txtIcon;
    @FXML
    public TextField txtFileName;
    @FXML
    public CButton btnSelectFile;
    @FXML
    public TextField txtURL;
    @FXML
    public ChoiceBox cbType;
    @FXML
    public CButton btnFromWorld;
    @FXML
    public CButton btnMultiple;
    @FXML
    public CButton btnSave;

    @FXML
    public void initialize(){
        cbType.getItems().clear();
        cbType.getItems().addAll(Translator.translate("mods.type.mod"), Translator.translate("mods.type.resource"), Translator.translate("mods.type.world"));
        cbType.getSelectionModel().select(0);

        btnSave.setOnMouseClicked(a -> {
            String name = txtName.getText();
            String icon = txtIcon.getText();
            String file = txtFileName.getText();
            String url = txtURL.getText();

            if (file == null || (!file.endsWith(".zip") && !file.endsWith(".jar"))){
                CMsgBox.msg(Alert.AlertType.ERROR, Translator.translate("error.oops"), Translator.translate("import.error.wrongFile")).show();
                return;
            }

            if (url == null || url.isBlank()){
                CMsgBox.msg(Alert.AlertType.ERROR, Translator.translate("error.oops"), Translator.translate("import.error.invalidURL")).show();
                return;
            }

            int index = cbType.getSelectionModel().getSelectedIndex();
            Profiler.getProfiler().setProfile(profile.getName(), x -> {
                if (index == 0)
                    x.getMods().add(setRes(file, url, name, icon, new Mod()));
                else if (index == 1)
                    x.getResources().add(setRes(file, url, name, icon, new Resourcepack()));
                else
                    x.getOnlineWorlds().add(setRes(file, url, name, icon, new World()));
            });

            refresh();
        });

        btnSelectFile.setOnMouseClicked(a -> {
            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP / JAR", "*.zip", "*.jar"));
            var path = switch (cbType.getSelectionModel().getSelectedIndex()){
                case 0 -> profile.getPath().to("mods");
                case 1 -> profile.getPath().to("resourcepacks");
                default -> profile.getPath().to("saves");
            };
            chooser.setInitialDirectory(path.toFile());
            var file = chooser.showOpenDialog(btnSelectFile.getScene().getWindow());
            if (file == null)
                return;
            txtFileName.setText(file.getName());
        });

        btnMultiple.setOnMouseClicked(a -> {
            MultipleMod.open(profile).show();
            FXManager.getManager().closeStage(btnMultiple.getScene().getWindow());
        });
    }

    public <T extends CResource> T setRes(String file, String url, String name, String icon, T res){
        res.fileName = file;
        res.fileUrl = url;
        res.name = name == null || name.isBlank() ? file : name;
        res.logoUrl = icon == null || icon.isBlank() ? null : icon;

        return res;
    }

    public void refresh(){
        txtName.setText(null);
        txtIcon.setText(null);
        txtFileName.setText(null);
        txtURL.setText(null);
        cbType.getSelectionModel().select(0);
    }
}
