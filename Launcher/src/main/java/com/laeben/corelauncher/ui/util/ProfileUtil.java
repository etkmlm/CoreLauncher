package com.laeben.corelauncher.ui.util;

import com.google.gson.*;
import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.entity.ImageEntity;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.FDObject;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.api.ui.UI;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;

public class ProfileUtil {
    public static void export(Profile profile, Window w){
        var chooser = new FileChooser();
        chooser.setInitialFileName(profile.getName() + ".json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        var file = chooser.showSaveDialog(w);
        if (file == null)
            return;
        exportJson(profile, Path.begin(file.toPath()));
    }

    public static void export(List<Profile> profiles, Window w){
        var chooser = new DirectoryChooser();
        var file = chooser.showDialog(w);
        if (file == null)
            return;
        profiles.forEach(a -> exportJson(a, Path.begin(file.toPath()).to(a.getName() + ".json")));
    }

    public static void exportJson(Profile profile, Path to){
        String read = Profiler.verifyProfileJsonIcon(profile);
        try{
            to.write(read);
            //profileJson.copy(Path.begin(file.toPath()));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

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
                    icon = pa.exists() ? ImageEntity.fromBase64(ImageCacheManager.encodeImage(pa)) : null;
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
        catch (Exception e){
            Logger.getLogger().log(e);
        }

    }

    public static void backup(Profile profile, Window w){
        var chooser = new FileChooser();
        chooser.setInitialFileName(profile.getName() + ".zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        var file = chooser.showSaveDialog(w);
        if (file == null)
            return;
        new Thread(() -> {
            try{
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.started"), Translator.translateFormat("announce.misc.profile", profile.getName()), Announcement.AnnouncementType.INFO), Duration.millis(1500)));
                Profiler.backup(profile, Path.begin(file.toPath()));
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.completed"), Translator.translateFormat("announce.misc.profile", profile.getName()), Announcement.AnnouncementType.INFO), Duration.millis(1500) ));
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }).start();
    }

    public static void backup(List<Profile> profiles, Window w){
        var chooser = new DirectoryChooser();
        var file = chooser.showDialog(w);
        if (file == null)
            return;
        new Thread(() -> {
            try{
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.started"), Translator.translateFormat("announce.info.backup.profiles.begin", profiles.size()), Announcement.AnnouncementType.INFO), Duration.millis(1500)));
                profiles.forEach(a -> Profiler.backup(a, Path.begin(file.toPath()).to(a.getName() + ".zip")));
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.completed"), Translator.translateFormat("announce.info.backup.profiles.end", profiles.size()), Announcement.AnnouncementType.INFO), Duration.millis(1500) ));
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }).start();
    }

    public static void backup(FDObject obj, Window w){
        if (obj.isSingle()){
            backup(obj.getProfiles().get(0), w);
            return;
        }

        var chooser = new FileChooser();
        String name = StrUtil.pure(obj.getName().trim());
        chooser.setInitialFileName(name + ".zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
        var file = chooser.showSaveDialog(w);
        if (file == null)
            return;


        new Thread(() -> {
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.started"), Translator.translateFormat("announce.misc.object", obj.getName()), Announcement.AnnouncementType.INFO), Duration.millis(1500)));
            var path = Path.begin(file.toPath());
            var tempFolder = path.parent().to(name);
            for (var p : obj.getProfiles()){
                var px = tempFolder.to(p.getName());
                p.getPath().copy(px);
                px.to("profile.json").write(Profiler.verifyProfileJsonIcon(p));
            }
            tempFolder.zip(path);
            tempFolder.delete();
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.backup.completed"), Translator.translateFormat("announce.misc.object", obj.getName()), Announcement.AnnouncementType.INFO), Duration.millis(1500)));
        }).start();
    }

    public static void importO(Path p, double x, double y){
        var temp = Configurator.getConfig().getTemporaryFolder();

        if (p.getExtension().equals("zip")){
            p.extract(temp, null);
            String name = p.getFirstZipEntry().replace("/", "");
            var exPath = temp.to(name);
            if (exPath.to("profile.json").exists()){
                /*String gen = Profiler.getProfiler().generateName(name);
                exPath.move(Profiler.profilesDir().to(gen));*/
                Profiler.getProfiler().importFromPath(exPath);
            }
            else{
                var ps = Profiler.getProfiler().importFromPath(exPath);
                String gen = FloatDock.getDock().generateName(name);
                var group = FDObject.createGroup(ps, x, y, gen);
                FloatDock.getDock().place(group, false);
            }

            return;
        }

        if (!p.getExtension().equals("json"))
            return;

        var obj = GsonUtil.DEFAULT_GSON.fromJson(p.read(), JsonObject.class);
        if (obj.has("type") && obj.get("type").getAsInt() == 1){
            var profiles = obj.get("profiles").getAsJsonArray().asList().stream().map(a -> GsonUtil.DEFAULT_GSON.fromJson(a, Profile.class)).toList();
            Profiler.getProfiler().importProfiles(profiles);

            String name = obj.get("name").getAsString();
            String gen = FloatDock.getDock().generateName(name);
            var group = FDObject.createGroup(profiles, x, y, gen);
            FloatDock.getDock().place(group, false);
            return;
        }

        Profiler.getProfiler().importFromPath(p);
    }
}
