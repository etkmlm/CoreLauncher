package com.laeben.corelauncher.ui.dialog;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.cell.CMCell;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.api.ui.UI;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DModSelector<T extends ModResource> extends CDialog<CResource> {

    private final T resource;
    private final Profile profile;

    private CResource installed;

    @FXML
    private CView icon;
    @FXML
    private Label lblName;
    @FXML
    private TextArea txtDesc;
    @FXML
    private TextField txtSearchVersion;
    @FXML
    private TextArea txtModpackContent;
    @FXML
    private VBox mpContentContainer;
    @FXML
    private HBox mpButtonContainer;
    @FXML
    private CButton btnClose;
    @FXML
    private CButton btnModpack;
    @FXML
    private CList<CResource> lvVersions;
    @FXML
    private CWorker<List<CResource>, Void> wModpack;

    public DModSelector(final T resource, Profile p) {
        super("layout/dialog/modselector.fxml", false);
        this.resource = resource;
        profile = p;

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        icon.setCornerRadius(128, 128, 8);
        if (resource.getIcon() != null && !resource.getIcon().isEmpty() && !resource.getIcon().equals("optifine"))
            icon.setImage(new Image(resource.getIcon(), true));

        txtSearchVersion.textProperty().addListener(a -> lvVersions.filter(txtSearchVersion.getText()));
        lblName.setText(resource.getName());
        txtDesc.setText(resource.getDescription() + "\n\n" + Translator.translateFormat("mods.author",String.join(",", resource.getAuthors())));
        btnClose.setOnMouseClicked(a -> {
            setResult(installed);
            close();
        });
        btnClose.enableTransparentAnimation();

        lblName.setCursor(Cursor.HAND);
        icon.setCursor(Cursor.HAND);
        lblName.setOnMouseClicked(a -> navigateWeb());
        icon.setOnMouseClicked(a -> navigateWeb());

        lvVersions.setFilterFactory(a -> a.query() != null && a.input().fileName.contains(a.query().toLowerCase(Locale.US)));
        lvVersions.setCellFactory(() -> new CMCell().setProfile(profile).setOnInstallClicked(a -> {
            if (a.isInstalled()){
                installed = null;
                Modder.getModder().remove(profile, a.getItem());
                return false;
            }

            try {
                if (a.getItem().getType() != ResourceType.MODPACK){
                    var items = resource.getSourceType().getSource().getDependencies(List.of(a.getItem()), ModSource.Options.create(profile).dependencies(true));
                    Modder.getModder().includeAll(profile, items);
                }
                else
                    Modder.getModder().include(profile, a.getItem());

                installed = a.getItem();
                lvVersions.getList().getChildren().forEach(x -> {
                    var cell = (CMCell)x;
                    if (!cell.equals(a))
                        cell.setInstalled(false);
                });
                return true;
            } catch (NoConnectionException | HttpException | StopException e) {
                Logger.getLogger().log(e);
            }

            return false;
        }));

        boolean b = resource.getResourceType() == ResourceType.MODPACK;
        mpButtonContainer.setVisible(b);
        mpButtonContainer.setManaged(b);
        mpContentContainer.setVisible(false);
        mpContentContainer.setManaged(false);
        if (b)
            wModpack.begin().withTask(a -> new Task<>() {
                    @Override
                    protected List<CResource> call() throws Exception {
                        return resource.getSourceType().getSource().getCoreResource(resource, ModSource.Options.create(profile).aggregateModpack());
                    }
            }).onDone(x -> {
                var mps = x.getValue();
                var mods = String.join("\n", mps.stream().filter(a -> a instanceof Mod).map(a -> a.name).toList());
                var ress = String.join("\n", mps.stream().filter(a -> a instanceof Resourcepack).map(a -> a.name).toList());
                var shaders = String.join("\n", mps.stream().filter(a -> a instanceof Shader).map(a -> a.name).toList());
                UI.runAsync(() ->{
                    txtModpackContent.setText(Translator.translateFormat("mods.all.content", mods, ress, shaders));
                    mpContentContainer.setVisible(true);
                    mpContentContainer.setManaged(true);
                    mpButtonContainer.setManaged(false);
                    mpButtonContainer.setVisible(false);
                    Main.getMain().clearAllStates();
                });
            });

        btnModpack.setOnMouseClicked(e -> wModpack.run());

    }

    public void navigateWeb(){
        if (!Desktop.isDesktopSupported())
            return;

        new Thread(() -> {
            try {
                Desktop.getDesktop().browse(new URI(resource.getURL()));
            } catch (Exception ignored) {

            }
        }).start();
    }

    public Optional<CResource> select(CResource installed) throws NoConnectionException, HttpException {
        this.installed = installed;
        lvVersions.getItems().setAll(resource.getSourceType().getSource().getAllCoreResources(resource, ModSource.Options.create(profile.getVersionId(), profile.getLoaderType(resource.getResourceType()))));
        lvVersions.load();
        return super.action();
    }


}
