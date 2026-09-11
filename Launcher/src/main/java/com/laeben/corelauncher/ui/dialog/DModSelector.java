package com.laeben.corelauncher.ui.dialog;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.context.EventContext;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.concurrency.Tasker;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.resource.*;
import com.laeben.corelauncher.minecraft.modding.event.ModdingContext;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.dialog.modselector.ModVersionCell;
import com.laeben.corelauncher.ui.util.DisplayUtil;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.util.entity.LogType;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Window;
import org.apache.commons.lang3.Strings;

import java.awt.*;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class DModSelector<T extends ModResource> extends CDialog<DModSelector.ModSelection> {

    private static final String TOGGLED_CLASS = "toggled";

    public record ModSelection(CResource resource, Profile profile) { }
    public record ModpackContent(CResource resource, String titleKey, SimpleIntegerProperty count) {

        public static ModpackContent mods(){
            return new ModpackContent(null, "mods.type.mods", new SimpleIntegerProperty());
        }

        public static ModpackContent resources(){
            return new ModpackContent(null, "mods.type.resources", new SimpleIntegerProperty());
        }

        public static ModpackContent shaders(){
            return new ModpackContent(null, "mods.type.shaders", new SimpleIntegerProperty());
        }
    }

    public static class ModpackContentCell extends ListCell<ModpackContent>{
        private final static Insets TITLE_INSETS = new Insets(16, 8, 16, 0);
        private final static Insets CONTENT_INSETS = new Insets(8, 8, 8, 8);

        private final HBox root;

        private final Label title;
        private final CView image;

        private String titleText;
        private ChangeListener<Number> countChangeListener;

        public ModpackContentCell(){
            root = new HBox();
            root.setSpacing(16);
            root.setAlignment(Pos.CENTER_LEFT);

            image = new CView();
            image.setCornerRadius(32, 32, 8);
            image.setFitWidth(32);
            image.setFitHeight(32);

            title = new Label();
            title.setOnMouseClicked(a -> {
                if (getItem().resource != null && getItem().resource.resourceUrl != null && a.getClickCount() >= 2){
                    try {
                        OSUtil.openURL(getItem().resource.resourceUrl);
                    } catch (IOException ignored) {

                    }
                }
            });

            root.getChildren().addAll(image, title);
        }

        private void onCountChange(Observable ob, Number o, Number n){
            title.setText(titleText + " (" + n + ")");
        }

        @Override
        public void updateItem(ModpackContent item, boolean empty){

            if (getItem() != null && countChangeListener != null){
                getItem().count().removeListener(countChangeListener);
                countChangeListener = null;
            }

            super.updateItem(item, empty);

            if (item == null || empty){
                setGraphic(null);
                return;
            }

            if (item.resource != null){
                if (item.resource.logoUrl == null) image.setImage((Image) null);
                else image.setImageAsync(ImageUtil.getNetworkImage(item.resource.logoUrl, 32, 32));
                image.setManaged(true);

                titleText = null;
                title.setText(item.resource.name);
                title.setStyle("-fx-font-size: 14pt;");

                root.setPadding(CONTENT_INSETS);
            }
            else if (item.titleKey != null){
                image.setImage((Image) null);
                image.setManaged(false);

                title.setText(titleText = Translator.translate(item.titleKey));
                title.setStyle("-fx-font-size: 16pt; -fx-font-weight: bold");
                countChangeListener = this::onCountChange;
                onCountChange(null, 0, item.count().get());
                item.count().addListener(countChangeListener);

                root.setPadding(TITLE_INSETS);
            }

            setGraphic(root);
        }
    }

    private final T resource;
    private final ResourcePreferences preferences;
    private final ObjectProperty<CResource> installed;

    private Profile newProfile;

    @FXML
    private CView icon;
    @FXML
    private Label lblName;
    @FXML
    private Text txtDesc;
    @FXML
    private TextField txtSearch;
    @FXML
    private CButton btnClose;
    @FXML
    private CWorker<List<CResource>, Void> wModpack;

    @FXML
    private ListView<CResource> lvVersions;
    private final ObservableList<CResource> versions;
    private final FilteredList<CResource> filteredVersions;

    @FXML
    private CButton btnCancel;
    @FXML
    private HBox statusContainer;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private Label lblStatus;

    private Tasker.TaskRecord installRecord;

    @FXML
    private HBox navContainer;
    @FXML
    private ListView<ModpackContent> lvModpackContent;
    @FXML
    private CButton btnNavVersions, btnNavMods, btnNavResources, btnNavShaders;

    private boolean contentsLoaded;
    private final ObservableList<ModpackContent> modpackContents;
    private final FilteredList<ModpackContent> filteredModpackContents;
    private int modsIndex, resourcesIndex, shadersIndex;

    private int scrollType;

    private void onProgress(long current, long total, EventContext context){
        if (total == 0) total = 1;

        final long finalTotal = total;

        UI.runAsync(() -> {
            final double prg = current * 1.0 / finalTotal;
            progress.setProgress(prg);
            progress.setVisible(prg != 0);
            progress.setManaged(prg != 0);

            if (current == 0){
                if (context instanceof ModdingContext ctx)
                    lblStatus.setText(Translator.translate(ctx.getLabel()));
                else
                    lblStatus.setText(null);
            }
            else if (current >= finalTotal)
                lblStatus.setText(null);
            else
                lblStatus.setText(DisplayUtil.parseDownloadProgress(current) + " / " + DisplayUtil.parseDownloadProgress(finalTotal));
        });
    }

    /**
     * @param type 0 - versions, 1 - mods, 2 - resourcepacks, 3 - shaders
     */
    private void toggleModpackContent(int type){
        if (type == 0){
            lvModpackContent.setVisible(false);
            lvModpackContent.setManaged(false);
            lvVersions.setVisible(true);
            lvVersions.setManaged(true);
            if (!filteredVersions.predicateProperty().isBound())
                filteredVersions.predicateProperty().bind(Bindings.createObjectBinding(
                    () -> r -> txtSearch.getText() != null && Strings.CI.contains(r.fileName, txtSearch.getText()),
                    txtSearch.textProperty()
                ));
            filteredModpackContents.predicateProperty().unbind();

            btnNavVersions.setStyle("-fx-background-color: -radio-checked-fill");

            return;
        }

        filteredVersions.predicateProperty().unbind();
        if (!filteredModpackContents.predicateProperty().isBound())
            filteredModpackContents.predicateProperty().bind(Bindings.createObjectBinding(
                () -> r -> r.resource == null || txtSearch.getText() != null && Strings.CI.contains(r.resource.name, txtSearch.getText()),
                txtSearch.textProperty()
            ));

        btnNavVersions.setStyle(null);
        lvModpackContent.setVisible(true);
        lvModpackContent.setManaged(true);
        lvVersions.setVisible(false);
        lvVersions.setManaged(false);

        if (!contentsLoaded){
            scrollType = type;
            wModpack.run();
        }
        else{
            lvModpackContent.scrollTo(switch (type){
                case 2 -> resourcesIndex;
                case 3 -> shadersIndex;
                default -> modsIndex;
            });
        }
    }

    private void install(CResource resource){
        if (resource == null) return;

        var profile = preferences.getProfile() == null ? null : preferences.getProfile();
        boolean isNewProfile = profile == null;

        if (resource.equals(installed.get())){
            UI.runAsync(() -> installed.set(null));
            if (!isNewProfile) Modder.getModder().remove(profile, resource);
            else if (newProfile != null) {
                Profiler.getProfiler().deleteProfile(newProfile);
                newProfile = null;
            }
            return;
        }

        if (isNewProfile){
            if (resource.targetVersionId == null || resource.targetLoader == null){
                Logger.getLogger().log(LogType.ERROR, "Required info could not be found to create a profile.");
                return;
            }

            try {
                profile = ResourcePreferences.createProfileFromPreferences(
                        preferences,
                        resource.targetVersionId,
                        List.of(LoaderType.fromIdentifier(resource.targetLoader)),
                        resource instanceof Modpack mp ? mp.name : null
                );

                newProfile = profile;
            } catch (PerformException | InvalidObjectException e) {
                Logger.getLogger().log(e);
                return;
            }
            catch (StopException ignored){
                return;
            }
        }


        UI.runAsync(() -> {
            statusContainer.setVisible(true);
            statusContainer.setManaged(true);
        });

        try {
            if (resource.getType() != ResourceType.MODPACK){
                var items = resource.getSourceType().getSource().getDependencies(List.of(resource), ModSource.Options.create(profile).dependencies(true));
                Modder.getModder().include(profile, items, this::onProgress);
            }
            else
                Modder.getModder().include(profile, List.of(resource), this::onProgress);

            UI.runAsync(() -> installed.set(resource));
            return;
        } catch (HttpException | IOException e) {
            Logger.getLogger().log(e);
        }
        catch (StopException | NoConnectionException ignored){

        }
        finally {
            UI.runAsync(() -> {
                statusContainer.setVisible(false);
                statusContainer.setManaged(false);
            });
        }

        // fail state

        if (isNewProfile){
            Profiler.getProfiler().deleteProfile(newProfile);
            newProfile = null;
        }
    }

    public DModSelector(final T resource, ResourcePreferences p, Window owner) {
        super("layout/dialog/modselector.fxml", false, owner);
        this.resource = resource;
        this.preferences = p;
        this.installed = new SimpleObjectProperty<>();

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        versions = FXCollections.observableArrayList();
        filteredVersions = new FilteredList<>(versions);
        lvVersions.setCellFactory(a -> new ModVersionCell()
                .setInstalledProperty(installed)
                .setOnInstallClicked(r -> {
                    if (installRecord != null) return;
                    installRecord = Tasker.getDefault().await(() -> install(r)).onFinished(() -> installRecord = null);
                })
        );
        lvVersions.setItems(filteredVersions);

        modpackContents = FXCollections.observableArrayList();
        filteredModpackContents = new FilteredList<>(modpackContents);
        lvModpackContent.setItems(filteredModpackContents);

        lvModpackContent.setCellFactory(a -> new ModpackContentCell());
        btnNavVersions.setOnMouseClicked(a -> toggleModpackContent(0));
        btnNavMods.setOnMouseClicked(a -> toggleModpackContent(1));
        btnNavResources.setOnMouseClicked(a -> toggleModpackContent(2));
        btnNavShaders.setOnMouseClicked(a -> toggleModpackContent(3));
        toggleModpackContent(0);

        btnCancel.setOnMouseClicked(a -> {
            if (installRecord != null) installRecord.stop();
        });

        icon.setCornerRadius(144, 144, 8);
        if (resource.getIcon() != null && !resource.getIcon().isEmpty() && !resource.getIcon().equals("optifine"))
            icon.setImageAsync(ImageUtil.getNetworkImage(resource.getIcon(), 144, 144));

        lblName.setText(resource.getName());
        txtDesc.setText(resource.getDescription() + "\n\n" + Translator.translateFormat("mods.author",String.join(",", resource.getAuthors())));
        btnClose.setOnMouseClicked(a -> {
            setResult(new ModSelection(installed.get(), newProfile));
            close();
        });
        btnClose.enableTransparentAnimation();

        lblName.setCursor(Cursor.HAND);
        icon.setCursor(Cursor.HAND);
        lblName.setOnMouseClicked(a -> navigateWeb());
        icon.setOnMouseClicked(a -> navigateWeb());

        boolean b = resource.getResourceType() == ResourceType.MODPACK;
        navContainer.setVisible(b);
        navContainer.setManaged(b);
        if (b)
            wModpack.begin().withTask(a -> new Task<>() {
                    @Override
                    protected List<CResource> call() throws Exception {
                        return resource.getSourceType().getSource().getCoreResource(resource,
                            ModSource.Options
                                .create(preferences)
                                .useCancellationToken(a.getCancellableToken())
                                .aggregateModpack()
                        );
                    }
            })
            .onFailed(x -> Logger.getLogger().log(x.getError()))
            .onDone(x -> {
                var mps = x.getValue();

                var mods = ModpackContent.mods();
                var resources = ModpackContent.resources();
                var shaders = ModpackContent.shaders();

                modpackContents.setAll(mods, resources, shaders);

                UI.runAsync(() -> {
                    modsIndex = 0;
                    resourcesIndex = 1;
                    shadersIndex = 2;
                    for (var r : mps){
                        if (r instanceof Mod) {
                            modpackContents.add(modsIndex + 1, new ModpackContent(r, null, null));
                            resourcesIndex++;
                            shadersIndex++;
                            mods.count().set(mods.count().get() + 1);
                        }
                        else if (r instanceof Resourcepack){
                            modpackContents.add(resourcesIndex + 1, new ModpackContent(r, null, null));
                            shadersIndex++;
                            resources.count().set(resources.count().get() + 1);
                        }
                        else {
                            modpackContents.add(shadersIndex + 1, new ModpackContent(r, null, null));
                            shaders.count().set(shaders.count().get() + 1);
                        }
                    }

                    contentsLoaded = true;

                    toggleModpackContent(scrollType);
                });
            })
            .finallyDo(x -> UI.runAsync(() -> Main.getMain().refreshStates()));
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

    public Optional<ModSelection> select(CResource installed) throws NoConnectionException, HttpException, IOException, StopException {
        this.installed.set(installed);

        ModSource.Options options;

        if (preferences.getProfile() == null)
            options = ModSource.Options.create(preferences.getGameVersions(), ModResource.getGlobalSafeLoaders(resource.getResourceType(), preferences.getLoaderTypes()));
        else{
            var profile = preferences.getProfile();
            options = ModSource.Options.create(profile.getVersionId(), ModResource.getGlobalSafeLoaders(resource.getResourceType(), profile.getLoader().getType()));
        }

        versions.setAll(resource.getSourceType().getSource().getAllCoreResources(resource, options));

        return super.action();
    }


}
