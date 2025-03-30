package com.laeben.corelauncher.ui.dialog;

import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Mod;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Resourcepack;
import com.laeben.corelauncher.minecraft.modding.entity.resource.World;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CStatusLabel;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DResourceSelector extends CDialog<List<CResource>>{

    private final Profile profile;

    @FXML
    private TextField txtName;
    @FXML
    private TextField txtIcon;
    @FXML
    private TextField txtFileName;
    @FXML
    private CButton btnSelectFile;
    @FXML
    private TextField txtURL;
    @FXML
    private ChoiceBox cbType;
    @FXML
    private CButton btnCancel;
    @FXML
    private CButton btnSave;
    @FXML
    private CButton btnDone;
    @FXML
    private CStatusLabel<Integer> lblStatus;

    private final List<CResource> resources;

    public DResourceSelector(Profile profile) {
        super("layout/dialog/resourceselector.fxml", true);

        this.profile = profile;

        cbType.getItems().clear();
        cbType.getItems().addAll(Translator.translate("mods.type.mod"), Translator.translate("mods.type.resourcepack"), Translator.translate("mods.type.world"));
        cbType.getSelectionModel().select(0);

        resources = new ArrayList<>();

        btnSelectFile.enableTransparentAnimation();
        btnSelectFile.setOnMouseClicked(a -> {
            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP / JAR", "*.zip", "*.jar"));
            var path = switch (cbType.getSelectionModel().getSelectedIndex()){
                case 0 -> profile.getPath().to("mods");
                case 1 -> profile.getPath().to("resourcepacks");
                default -> profile.getPath().to("saves");
            };
            if (!path.exists())
                path.prepare();
            chooser.setInitialDirectory(path.toFile());
            var file = chooser.showOpenDialog(btnSelectFile.getScene().getWindow());
            if (file == null)
                return;
            txtFileName.setText(file.getName());
        });

        btnSave.enableTransparentAnimation();
        btnSave.setOnMouseClicked(a -> save());

        btnDone.enableTransparentAnimation();
        btnDone.setOnMouseClicked(a -> {
            if (resources.isEmpty())
                save();

            if (resources.isEmpty())
                return;

            close(resources);
        });

        btnCancel.enableTransparentAnimation();
        btnCancel.setOnMouseClicked(a -> close(null));

        lblStatus.setIsEmpty(a -> a == null || a == 0);
        lblStatus.setTextFactory(a -> Translator.translateFormat("import.added", a));
        lblStatus.setDefaultValue(0);
    }

    private void save(){
        String name = txtName.getText();
        String icon = txtIcon.getText();
        String file = txtFileName.getText();
        String url = txtURL.getText();

        if (file == null || (!file.endsWith(".zip") && !file.endsWith(".jar"))){
            Main.getMain().announceLater(Translator.translate("error.oops"), Translator.translate("import.error.wrongFile"), Announcement.AnnouncementType.ERROR, Duration.seconds(2));
            return;
        }

        if (url == null || url.isBlank()){
            Main.getMain().announceLater(Translator.translate("error.oops"), Translator.translate("import.error.invalidURL"), Announcement.AnnouncementType.ERROR, Duration.seconds(2));
            return;
        }

        if (resources.stream().anyMatch(x -> x.fileName.equals(file))){
            Main.getMain().announceLater(Translator.translate("error.oops"), Translator.translate("import.error.same"), Announcement.AnnouncementType.ERROR, Duration.seconds(2));
            return;
        }

        int index = cbType.getSelectionModel().getSelectedIndex();
        CResource res = switch (index){
            case 0 -> applyRes(file, url, name, icon, new Mod());
            case 1 -> applyRes(file, url, name, icon, new Resourcepack());
            case 2 -> applyRes(file, url, name, icon, new World());
            default -> null;
        };

        resources.add(res);
        lblStatus.setValue(lblStatus.getValue()+1);

        refresh();
    }

    private void close(List<CResource> result){
        setResult(result);
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        close();
    }

    private  <T extends CResource> T applyRes(String file, String url, String name, String icon, T res){
        res.fileName = file;
        res.fileUrl = url;
        res.name = name == null || name.isBlank() ? file : name;
        res.logoUrl = icon == null || icon.isBlank() ? null : icon;
        res.setMeta(false);

        return res;
    }

    public void refresh(){
        txtName.setText(null);
        txtIcon.setText(null);
        txtFileName.setText(null);
        txtURL.setText(null);
        cbType.getSelectionModel().select(0);
        getDialogPane().getButtonTypes().clear();
    }

    public Optional<List<CResource>> execute(){
        refresh();

        //getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.YES);

        return super.action();
    }
}
