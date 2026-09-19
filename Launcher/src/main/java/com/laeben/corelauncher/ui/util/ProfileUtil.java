package com.laeben.corelauncher.ui.util;

import com.google.gson.*;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.corelauncher.api.concurrency.TaskRecord;
import com.laeben.corelauncher.api.concurrency.Tasker;
import com.laeben.corelauncher.api.entity.*;
import com.laeben.corelauncher.api.shortcut.Shortcut;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.util.entity.LogType;
import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public class ProfileUtil {
    /**
     * Exports a profile, utilizes a dialog, unlike {@link #exportJson(Profile, Path)}.
     * @param profile target profile
     * @param w window for the dialog
     */
    public static void export(Profile profile, Window w){
        var chooser = new FileChooser();
        chooser.setInitialFileName(profile.getName() + ".json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        var file = chooser.showSaveDialog(w);
        if (file == null)
            return;
        exportJson(profile, Path.begin(file.toPath()));
    }

    /**
     * Exports multiple profiles into separate JSON files.
     * @param profiles target profiles
     * @param w window for the dialog
     */
    public static void export(List<Profile> profiles, Window w){
        var chooser = new DirectoryChooser();
        var file = chooser.showDialog(w);
        if (file == null)
            return;
        profiles.forEach(a -> exportJson(a, Path.begin(file.toPath()).to(a.getName() + ".json")));
    }

    /**
     * Exports a profile.
     * @param profile target profile
     * @param to exact JSON path to write
     */
    public static void exportJson(Profile profile, Path to){
        try{
            String read = Profiler.getBackupProfileJson(profile);
            to.write(read);
            //profileJson.copy(Path.begin(file.toPath()));
        }
        catch (StopException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    /**
     * Exports a float dock object (including groups) into a single JSON file.
     * @param obj object
     * @param w window for the dialog
     */
    public static void export(FDObject obj, Window w){
        if (obj.isSingle()){
            export(obj.getProfiles().get(0), w);
            return;
        }

        var chooser = new FileChooser();
        chooser.setInitialFileName(obj.getName() + ".json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        var file = chooser.showSaveDialog(w);
        if (file == null)
            return;

        var json = new JsonObject();
        json.add("name", new JsonPrimitive(obj.getName()));
        json.add("type", new JsonPrimitive(1));

        var arr = new JsonArray();
        for (var p : obj.getProfiles()){
            var js = (JsonObject) GsonUtil.DEFAULT_GSON.toJsonTree(p);
            if (js.has("icon")){
                var icon = GsonUtil.DEFAULT_GSON.fromJson(js.get("icon"), ImageEntity.class);
                if (!icon.isNetwork() && icon.getUrl() == null){
                    var pa = icon.getPath(Configurator.getConfig().getImagePath());
                    try {
                        icon = pa.exists() ? ImageEntity.fromBase64(ImageCacheManager.encodeImage(pa)) : null;
                    } catch (StopException ignored) {
                        icon = null;
                    } catch (IOException e) {
                        Logger.getLogger().log("Icon could not be exported for profile " + p.getName(), e);
                        icon = null;
                    }
                }
                js.add("icon", GsonUtil.DEFAULT_GSON.toJsonTree(icon));
            }
            arr.add(js);
        }

        json.add("profiles", arr);

        try{
            var path = Path.begin(file.toPath());
            path.write(GsonUtil.DEFAULT_GSON.toJson(json));
        }
        catch (StopException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    /**
     * Backs up a profile.
     * <br/>
     * Uses nested awaiting.
     * @param w window for the dialog
     */
    public static TaskRecord backup(Profile profile, Window w){
        var chooser = new FileChooser();
        chooser.setInitialFileName(profile.getName() + ".zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        var file = chooser.showSaveDialog(w);
        if (file == null)
            return null;

        return Tasker.getDefault().awaitNested(() -> {
            try{
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.started"), Translator.translateFormat("announce.misc.profile", profile.getName()), Announcement.AnnouncementType.INFO), Duration.millis(1500)));
                Profiler.backup(profile, Path.begin(file.toPath()), null); // TODO implement progress panel
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.completed"), Translator.translateFormat("announce.misc.profile", profile.getName()), Announcement.AnnouncementType.INFO), Duration.millis(1500) ));
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });
    }

    /**
     * Backs up multiple profiles.
     * <br/>
     * Uses nested awaiting.
     * @param w window for the dialog
     */
    public static TaskRecord backup(List<Profile> profiles, Window w){
        var chooser = new DirectoryChooser();
        var file = chooser.showDialog(w);
        if (file == null)
            return null;

        return Tasker.getDefault().await(() -> {
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.started"), Translator.translateFormat("announce.info.backup.profiles.begin", profiles.size()), Announcement.AnnouncementType.INFO), Duration.millis(1500)));
            int count = 0;
            for (var a : profiles){
                try{
                    Profiler.backup(a, Path.begin(file.toPath()).to(a.getName() + ".zip"), null); // TODO implement progress panel
                    count++;
                }
                catch (StopException ignored){
                    return;
                }
                catch (Exception e){
                    Logger.getLogger().log(e);
                }
            }

            final int backedUp = count;
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.completed"), Translator.translateFormat("announce.info.backup.profiles.end", backedUp), Announcement.AnnouncementType.INFO), Duration.millis(1500) ));
        });
    }

    /**
     * Backs up a float dock object.
     * <br/>
     * Uses nested awaiting.
     * @param obj object
     * @param w window for the dialog
     */
    public static TaskRecord backup(FDObject obj, Window w){
        if (obj.isSingle()) return backup(obj.getProfiles().get(0), w);

        var chooser = new FileChooser();
        String name = StrUtil.pure(obj.getName().trim());
        chooser.setInitialFileName(name + ".zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        var file = chooser.showSaveDialog(w);
        if (file == null)
            return null;

        return Tasker.getDefault().awaitNested(() -> {
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.started"), Translator.translateFormat("announce.misc.object", obj.getName()), Announcement.AnnouncementType.INFO), Duration.millis(1500)));
            try{
                var path = Path.begin(file.toPath());
                var tempFolder = path.parent().to(name);
                for (var p : obj.getProfiles()){
                    var px = tempFolder.to(p.getName());
                    p.getPath().copy(px);
                    px.to("profile.json").write(Profiler.getBackupProfileJson(p));
                }
                tempFolder.zip(path, null); // TODO implement progress panel
                tempFolder.delete();
            }
            catch (StopException ignored){
                return;
            }
            catch (Exception e){
                Logger.getLogger().log(e);
                return;
            }
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.completed"), Translator.translateFormat("announce.misc.object", obj.getName()), Announcement.AnnouncementType.INFO), Duration.millis(1500)));
        });
    }

    /**
     * Imports an object. (ZIP backup or JSON export)
     * <br/>
     * Uses nested awaiting.
     * @param files files to be imported from
     * @param x target dock x
     * @param y target dock y
     */
    public static TaskRecord importO(List<Path> files, double x, double y){
        return Tasker.getDefault().awaitNested(() -> {
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.import.started"), Translator.translateFormat("announce.info.import.importing", files.size()), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
            int c = 0;
            for (var path : files){
                try{
                    importO(path, x, y, null); // TODO implement progress panel
                    c++;
                }
                catch (CancellationException ignored){
                    break;
                }
                catch (Exception e){
                    Logger.getLogger().log(e);
                }
            }
            int fC = c;
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.import.completed"), Translator.translateFormat("announce.info.import.imported", fC), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
        });
    }


    /**
     * Imports an object. (ZIP backup or JSON export)
     * <br/>
     * Uses nested awaiting.
     * @param p path to be imported from
     * @param x target dock x
     * @param y target dock y
     * @param onProgress progress function
     * @exception UnsupportedOperationException if the path was other than ZIP or JSON
     */
    public static TaskRecord importO(Path p, double x, double y, ProgressFunction onProgress) throws Exception {
        var temp = Configurator.getConfig().getTemporaryFolder();

        if (p.isDirectory())
            return null;

        return Tasker.getDefault().awaitNested(() -> {
            if (p.getExtension().equals("zip")){
                p.extract(temp, null, onProgress);
                String name = p.getFirstZipEntry().replace("/", "");
                var exPath = temp.to(name);
                if (exPath.to("profile.json").exists()){
                    /*String gen = Profiler.getProfiler().generateName(name);
                    exPath.move(Profiler.profilesDir().to(gen));*/
                    Profiler.getProfiler().importFromPath(exPath, ProfileUtil::determineProfileOverwrite);
                }
                else{
                    var ps = Profiler.getProfiler().importFromPath(exPath, ProfileUtil::determineProfileOverwrite);
                    String gen = FloatDock.getDock().generateName(name);
                    var group = FDObject.createGroup(ps, x, y, gen);
                    FloatDock.getDock().place(group, false);
                }
            }
            else if (p.getExtension().equals("json")){
                var obj = Profile.PROFILE_GSON.fromJson(p.read(), JsonObject.class);
                if (obj.has("type") && obj.get("type").getAsInt() == 1){
                    var profiles = obj.get("profiles").getAsJsonArray().asList().stream().map(a -> GsonUtil.DEFAULT_GSON.fromJson(a, Profile.class)).toList();
                    Profiler.getProfiler().importProfiles(profiles, ProfileUtil::determineProfileOverwrite);

                    String name = obj.get("name").getAsString();
                    String gen = FloatDock.getDock().generateName(name);
                    var group = FDObject.createGroup(profiles, x, y, gen);
                    FloatDock.getDock().place(group, false);
                    return null;
                }

                Profiler.getProfiler().importFromPath(p, ProfileUtil::determineProfileOverwrite);
            }
            else throw new UnsupportedOperationException();

            return null;
        });
    }

    /**
     * Determines the profile overwriting supported with UI dialogs.
     * <br/>
     * Can be executed outside the UI thread.
     * @param p the profile to be overwritten
     */
    private static boolean determineProfileOverwrite(Profile p) {
        if (Configurator.getConfig().isOverwriteImportedEnabled())
            return true;

        final var r = UI.runSync(() -> {
            var oldProfile = Profiler.getProfiler().getProfile(p.getName());

            if (oldProfile == null){
                Logger.getLogger().log(LogType.ERROR, "Profile '" + p.getName() + "' could not be found.");
                return false;
            }

            var added = new ArrayList<String>();
            var removed = oldProfile.getAllResources().stream().map(a -> a.fileName != null ? a.fileName : a.name).collect(Collectors.toList());

            for(var n : p.getAllResources()){
                removed.remove(n.fileName == null ? n.name : n.fileName);
                var find = oldProfile.getAllResources().stream().filter(a -> a.isSameResource(n)).findFirst();
                if (find.isPresent()){
                    if (n.fileName != null && !n.fileName.equals(find.get().fileName)){
                        added.add(n.fileName);
                    }
                }
                else {
                    added.add(n.fileName == null ? n.name : n.fileName);
                }
            }

            String translate = Translator.translateFormat("import.ask.overwrite",
                    p.getName(),
                    String.join(",", removed),
                    String.join(",", added)
            );

            var result = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), translate)
                    .setButtons(CMsgBox.ResultType.ALWAYS_YES, CMsgBox.ResultType.YES, CMsgBox.ResultType.NO,  CMsgBox.ResultType.CANCEL)
                    .executeForResult();
            if (result.isEmpty() || result.get().result() == CMsgBox.ResultType.CANCEL)
                //throw new CancellationException();
                return null;

            if (result.get().result() == CMsgBox.ResultType.ALWAYS_YES){
                Configurator.getConfig().setOverwriteImported(true);
                Configurator.save();
            }

            return result.get().result().isPositive();
        });

        if (r == null)
            throw new CancellationException();

        return r;
    }

    /**
     * Creates an OS shortcut of a profile.
     */
    public static void createShortcut(Profile p, Window w){
        var chooser = new FileChooser();
        String extension = Shortcut.getExtension(OS.getSystemOS());
        chooser.setInitialFileName(p.getName() + extension);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension, "*" + extension));
        var file = chooser.showSaveDialog(w);
        if (file == null)
            return;

        try {
            Profiler.createShortcut(p, Path.begin(file.toPath()));
        } catch (URISyntaxException | IOException e) {
            Logger.getLogger().log(LogType.ERROR, "Cannot create a shortcut: The launcher does not running from a JAR file!");
            //Logger.getLogger().log(e);
        } catch (StopException ignored) {

        }
    }
}
