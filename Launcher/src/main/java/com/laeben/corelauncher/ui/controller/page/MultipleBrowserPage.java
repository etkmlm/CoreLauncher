package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.nbt.entity.NBTList;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.wrapper.forge.Forge;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.browser.CResourceCell;
import com.laeben.corelauncher.ui.controller.browser.Search;
import com.laeben.corelauncher.ui.controller.browser.SearchManager;
import com.laeben.corelauncher.ui.control.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultipleBrowserPage extends HandlerController {
    private record Result(List<CResource> found, List<String> unfound){}
    public record LocalResult(List<Modder.ModInfo> found, List<Path> unfound, List<Modder.ModInfo> invalid){}

    private final ObservableList<CResource> resources;

    private final SearchManager manager;

    public MultipleBrowserPage() {
        super("pgmbrowser");

        resources = FXCollections.observableArrayList();

        registerHandler(Profiler.getProfiler().getHandler(), a -> {
            if (a.getKey().equals("profileUpdate"))
                reload();
        }, true);

        manager = new SearchManager();
    }

    private void reloadTitle(Profile p){
        ((CTab)parentObj).setText(Translator.translate("frame.title.worlds") + " - " + StrUtil.sub(p.getName(), 0, 30));
    }

    public void setProfile(Profile profile){
        this.profile = profile;

        reload();
    }

    public void reload() {
        cbSource.setValue(null);
        cbMainType.setValue(null);
        manager.add(profile, ModSource.Type.values());

        lblProfileName.setText(profile.getName());
        reloadTitle(profile);
        profileIcon.setImage(CoreLauncherFX.getImageFromProfile(profile, 32, 32));

        btnBack.enableTransparentAnimation();
        btnBack.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile));
        btnBack.setText("⤶ " + Translator.translate("option.back"));
    }

    @FXML
    private CButton btnBack;
    @FXML
    private CView profileIcon;
    @FXML
    private Label lblProfileName;

    @FXML
    private TextArea txtQuery;
    @FXML
    private ListView<CResource> lvResources;
    @FXML
    private CCombo<ModSource> cbSource;
    @FXML
    private CCombo<ResourceType> cbMainType;
    @FXML
    private CButton btnSearch;
    @FXML
    private CButton btnFromWorld;
    @FXML
    private CButton btnFromFolder;
    @FXML
    private CButton btnApply;
    @FXML
    private CButton btnClear;
    @FXML
    private Label lblStatus;
    @FXML
    private CWorker<Result, String> worker;

    private Profile profile;

    private Search getSearch(){
        if (cbSource.getSelectedItem() == null)
            return null;
        var s = cbSource.getSelectedItem().getSearch(profile);
        s.reset();
        return s;
    }

    public static LocalResult loadFromPath(LoaderType type, List<Path> files){
        var mods = new ArrayList<Modder.ModInfo>();
        var un = new ArrayList<Path>();
        var invalid = new ArrayList<Modder.ModInfo>();

        for (var f : files){
            if (f.isDirectory()){
                var x = loadFromPath(type, f.getFiles());
                mods.addAll(x.found());
                un.addAll(x.unfound());
                invalid.addAll(x.invalid());
                continue;
            }

            var mod = Modder.getModder().getModFromJarFile(f);
            if (mod == null){
                un.add(f);
                continue;
            }

            if(mod.type() != type){
                invalid.add(mod);
                continue;
            }

            mods.add(mod);
        }

        return new LocalResult(mods, un, invalid);
    }

    public void loadFromResult(LocalResult result){
        result.found().forEach(x -> println(x.name()));
        result.unfound().forEach(x -> println(x.getNameWithoutExtension()));
    }

    @Override
    public void preInit() {
        profileIcon.setCornerRadius(32, 32, 8);

        btnFromFolder.enableTransparentAnimation();
        btnFromWorld.enableTransparentAnimation();
        btnApply.enableTransparentAnimation();
        btnSearch.enableTransparentAnimation();

        lvResources.setItems(resources);
        lvResources.setCellFactory(x -> new CResourceCell().setOnDelete(a -> resources.remove(a.getItem())));

        worker.begin().withTask(a -> new Task<>() {
            @Override
            protected Result call() throws Exception {
                var text = txtQuery.getText();
                if (text == null || text.isBlank())
                    return new Result(List.of(), List.of());
                var lines = text.lines().filter(x -> !x.isBlank()).toList();

                var list = new ArrayList<CResource>();
                var secondary = new ArrayList<String>();

                int i = 1;
                int size = lines.size();

                for (var line : lines) {
                    var x = manager.search(line, cbMainType.getSelectedItem(), getSearch());

                    a.setTaskStatus(i++ + " / " + size);

                    if (x == null || x.isEmpty()) {
                        secondary.add(line);
                        continue;
                    }

                    var res = x.get(0).resource();

                    list.addAll(res.getSourceType().getSource().getCoreResource(res, ModSource.Options.create(profile).dependencies(true)));
                }

                return new Result(list, secondary);
            }
        })
        .onStatus(a -> Platform.runLater(() -> lblStatus.setText(a.getStatus())))
        .onDone(a -> Platform.runLater(() -> {
            txtQuery.setText(null);
            resources.addAll(a.getValue().found().stream().distinct().toList());
            a.getValue().unfound().forEach(this::println);
            a.setTaskStatus(null);
        })).onFailed(a -> {
            a.setTaskStatus(null);
            if (!Main.getMain().announceLater(a.getError(), Duration.seconds(2)))
                Logger.getLogger().log(a.getError());
        });

        btnClear.setOnMouseClicked(a -> resources.clear());

        btnSearch.setOnMouseClicked(a -> worker.run());

        btnApply.setOnMouseClicked(a -> {
            try {
                Modder.getModder().includeAll(profile, resources);
            } catch (NoConnectionException | StopException ignored) {

            } catch (HttpException e) {
                Logger.getLogger().log(e);
            }
            Main.getMain().replaceTab(this, "pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile);
        });

        btnFromWorld.setOnMouseClicked(a -> {
            if (!(profile.getWrapper() instanceof Forge)){
                Main.getMain().announceLater(Translator.translate("error.oops"), Translator.translate("import.multiple.forge"), Announcement.AnnouncementType.ERROR, Duration.seconds(2));
                return;
            }

            var chooser = new DirectoryChooser();
            chooser.setInitialDirectory(profile.getPath().to("saves").prepare().toFile());
            var file = chooser.showDialog(btnFromWorld.getScene().getWindow());

            if (file == null)
                return;

            var path = Path.begin(file.toPath());
            var dat = path.to("level.dat");
            if (!dat.exists())
                return;

            var nbt = World.openNBT(dat);
            if (nbt == null)
                return;

            NBTList fml;
            var n = nbt.first().asCompound().firstForName("FML");
            if (n == null){
                n = nbt.first().asCompound().firstForName("fml");
                fml = (NBTList) n.asCompound().firstForName("LoadingModList");
            }
            else
                fml = (NBTList) n.asCompound().firstForName("ModList");

            var ids = fml.getItems().stream().map(x -> x.asCompound().firstForName("ModId").stringValue()).filter(x -> !x.equals("minecraft") && !x.equals("forge") && !x.equalsIgnoreCase("fml") && !x.equals("mcp")).toList();

            txtQuery.setText((txtQuery.getText() != null && !txtQuery.getText().isBlank() ? txtQuery.getText() + "\n\r" : "") + String.join("\n\r", ids));
        });
        btnFromFolder.setOnMouseClicked(a -> {
            var chooser = new DirectoryChooser();
            chooser.setInitialDirectory(profile.getPath().to("mods").prepare().toFile());
            var file = chooser.showDialog(btnFromWorld.getScene().getWindow());

            if (file == null)
                return;

            var path = Path.begin(file.toPath());

            loadFromResult(loadFromPath(profile.getWrapper().getType(), path.getFiles()));
            //var files = ps.stream().filter(x -> x.getExtension() != null && x.getExtension().equals("jar")).map(x -> x.getNameWithoutExtension().split("-")[0]).toList();
            //files.forEach(this::println);
        });

        cbSource.getItems().setAll(Arrays.stream(ModSource.Type.values()).map(ModSource.Type::getSource).toList());
        cbSource.setValueFactory(a -> a.getType().name());

        cbMainType.getItems().setAll(ResourceType.MOD, ResourceType.WORLD, ResourceType.SHADER, ResourceType.RESOURCE);
        cbMainType.setValueFactory(a -> Translator.translate("mods.type." + a.getName()));
    }

    private void println(String text){
        String f = txtQuery.getText();
        if (f != null && !txtQuery.getText().isBlank())
            f += "\n\r";

        txtQuery.setText((f == null ? "" : f) + text);
    }
}
