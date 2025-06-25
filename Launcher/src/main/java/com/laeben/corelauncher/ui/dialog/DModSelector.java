package com.laeben.corelauncher.ui.dialog;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Mod;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Resourcepack;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Shader;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.cell.CMCell;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.util.entity.LogType;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DModSelector<T extends ModResource> extends CDialog<DModSelector.ModSelection> {

    public record ModSelection(CResource resource, Profile profile){

    }

    private final T resource;
    private final ResourcePreferences preferences;

    private final Executor executor;

    private CResource installed;
    private Profile newProfile;

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
    private ScrollPane scroll;
    @FXML
    private CList<CResource> lvVersions;
    @FXML
    private CWorker<List<CResource>, Void> wModpack;

    public DModSelector(final T resource, ResourcePreferences p) {
        super("layout/dialog/modselector.fxml", false);
        this.resource = resource;
        preferences = p;

        executor = Executors.newSingleThreadExecutor();

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        icon.setCornerRadius(128, 128, 8);
        if (resource.getIcon() != null && !resource.getIcon().isEmpty() && !resource.getIcon().equals("optifine"))
            icon.setImageAsync(ImageUtil.getNetworkImage(resource.getIcon(), 128, 128));

        txtSearchVersion.textProperty().addListener(a -> lvVersions.filter(txtSearchVersion.getText()));
        lblName.setText(resource.getName());
        txtDesc.setText(resource.getDescription() + "\n\n" + Translator.translateFormat("mods.author",String.join(",", resource.getAuthors())));
        btnClose.setOnMouseClicked(a -> {
            setResult(new ModSelection(installed, newProfile));
            close();
        });
        btnClose.enableTransparentAnimation();

        lblName.setCursor(Cursor.HAND);
        icon.setCursor(Cursor.HAND);
        lblName.setOnMouseClicked(a -> navigateWeb());
        icon.setOnMouseClicked(a -> navigateWeb());

        lvVersions.setLoadLimit(15);
        lvVersions.setFilterFactory(a -> a.query() != null && a.input().fileName.contains(a.query().toLowerCase(Locale.US)));
        lvVersions.setCellFactory(() -> new CMCell().setOnTestExistance(a -> {
            if (preferences.getProfile() != null){
                var res = preferences.getProfile().getResource(a.getItem().id);
                return res != null && res.fileName != null && res.fileName.equals(a.getItem().fileName);
            }
            else
                return false;
        }).setOnInstallClicked(a -> executor.execute(() -> {
            var profile = preferences.getProfile() == null ? null : preferences.getProfile();

            boolean isNewProfile = false;

            if (profile == null){
                if (a.getItem().targetVersionId == null || a.getItem().targetLoader == null){
                    Logger.getLogger().log(LogType.ERROR, "Required info could not be found to create a profile.");
                    return;
                }

                try {
                    profile = ResourcePreferences.createProfileFromPreferences(preferences, a.getItem().targetVersionId, LoaderType.fromIdentifier(a.getItem().targetLoader), null);
                    isNewProfile = true;
                } catch (PerformException e) {
                    Logger.getLogger().log(e);
                    return;
                }
            }

            if (isNewProfile)
                newProfile = profile;

            if (a.isInstalled()){
                installed = null;
                if (!isNewProfile)
                    Modder.getModder().remove(profile, a.getItem());
                UI.runAsync(() -> a.setInstalled(false));
                return;
            }

            try {
                if (a.getItem().getType() != ResourceType.MODPACK){
                    var items = resource.getSourceType().getSource().getDependencies(List.of(a.getItem()), ModSource.Options.create(profile).dependencies(true));
                    Modder.getModder().include(profile, items);
                }
                else
                    Modder.getModder().include(profile, List.of(a.getItem()));

                installed = a.getItem();
                UI.runAsync(() -> lvVersions.getList().getChildren().forEach(x -> {
                    var cell = (CMCell)x;
                    cell.setInstalled(cell.equals(a));
                }));
                return;
            } catch (NoConnectionException | HttpException | StopException e) {
                Logger.getLogger().log(e);
            }

            UI.runAsync(() -> a.setInstalled(false));
        })));

        boolean b = resource.getResourceType() == ResourceType.MODPACK;
        mpButtonContainer.setVisible(b);
        mpButtonContainer.setManaged(b);
        mpContentContainer.setVisible(false);
        mpContentContainer.setManaged(false);
        if (b)
            wModpack.begin().withTask(a -> new Task<>() {
                    @Override
                    protected List<CResource> call() throws Exception {
                        return resource.getSourceType().getSource().getCoreResource(resource, ModSource.Options.create(preferences).aggregateModpack());
                    }
            }).onDone(x -> {
                var mps = x.getValue();
                var mods = mps.stream().filter(a -> a instanceof Mod).map(a -> a.name).toList();
                var ress = mps.stream().filter(a -> a instanceof Resourcepack).map(a -> a.name).toList();
                var shaders = mps.stream().filter(a -> a instanceof Shader).map(a -> a.name).toList();
                UI.runAsync(() -> {
                    txtModpackContent.setText(Translator.translateFormat("mods.all.content", mods.size(), String.join("\n", mods), ress.size(), String.join("\n", ress), shaders.size(), String.join("\n", shaders)));
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

    public Optional<ModSelection> select(CResource installed) throws NoConnectionException, HttpException {
        this.installed = installed;

        ModSource.Options options;

        if (preferences.getProfile() == null)
            options = ModSource.Options.create(preferences.getGameVersions(), ModResource.getGlobalSafeLoaders(resource.getResourceType(), preferences.getLoaderTypes()));
        else{
            var profile = preferences.getProfile();
            options = ModSource.Options.create(profile.getVersionId(), ModResource.getGlobalSafeLoaders(resource.getResourceType(), profile.getWrapper().getType()));
        }

        lvVersions.getItems().setAll(resource.getSourceType().getSource().getAllCoreResources(resource, options));
        lvVersions.load();

        scroll.vvalueProperty().addListener((a, b, c) -> {
            if (b.equals(c) || scroll.getVmax() > c.doubleValue())
                return;
            lvVersions.load();
        });

        return super.action();
    }


}
