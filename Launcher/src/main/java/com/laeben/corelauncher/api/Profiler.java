package com.laeben.corelauncher.api;

import com.laeben.core.entity.Path;
import com.laeben.core.util.events.ChangeEvent;
import com.laeben.corelauncher.api.entity.ImageEntity;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.util.ImageCacheManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Profiler {
    public static final String PROFILE_CREATE = "profileCreate";
    public static final String PROFILE_UPDATE = "profileUpdate";
    public static final String PROFILE_DELETE = "profileDelete";

    private static Profiler instance;
    private Path profilesDir;
    private List<Profile> profiles;
    private final EventHandler<ChangeEvent> handler;

    public Profiler() {
        profilesDir = profilesDir();

        handler = new EventHandler<>();

        Configurator.getConfigurator().getHandler().addHandler("profiler", (a) -> {
            if (!a.getKey().equals(Configurator.GAME_PATH_CHANGE))
                return;
            profilesDir = profilesDir();

            reload();
        }, false);

        instance = this;
    }

    public static Path profilesDir(){
        return Configurator.getConfig().getLauncherPath().to("profiles");
    }

    public EventHandler<ChangeEvent> getHandler(){
        return handler;
    }

    public static Profiler getProfiler(){
        return instance;
    }

    /**
     * Move profiles to the current game path from the old game path.
     * @param oldGamePath the old game path
     */
    public void moveProfiles(Path oldGamePath){
        try{
            oldGamePath.to("launcher", "profiles").getFiles().forEach(x -> x.copy(profilesDir.to(x.getName())));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public List<Profile> getAllProfiles(){
        return profiles;
    }

    public Profile getProfile(String name){
        return profiles.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(Profile.empty());
    }

    /**
     * Backup the profile to the target zip file.
     * @param p the profile
     * @param to target zip file
     */
    public static void backup(Profile p, Path to){
        var path = p.getPath();
        var jsPath = path.to("profile.json");

        String first = jsPath.read();
        String json = verifyProfileJsonIcon(p);

        jsPath.write(json);
        path.zip(to);
        jsPath.write(first);
    }

    private Profile importFromBackup(Path p){
        var profile = GsonUtil.DEFAULT_GSON.fromJson(p.read(), Profile.class);
        String gen = generateName(profile.getName());
        p.parent().move(profilesDir.to(gen));
        verifyProfileIcon(profile);
        profile.setName(gen).save();

        profiles.add(profile);

        return profile;
    }

    /**
     * Import profile from the zip or JSON file.
     * @param path zip or JSON file
     */
    public List<Profile> importFromPath(Path path){
        var ext = path.getExtension();
        var tempProfiles = Configurator.getConfig().getTemporaryFolder();

        if (path.isDirectory()){
            var pxn = path.to("profile.json");

            var list = new ArrayList<Profile>();

            if (pxn.exists())
                list.add(importFromBackup(pxn));
            else{
                for (var i : path.getFiles()){
                    if (!i.isDirectory())
                        continue;
                    var px = i.to("profile.json");
                    if (!px.exists())
                        continue;
                    var pr = importFromBackup(px);
                    list.add(pr);
                }
            }

            //profiles.addAll(list);

            handler.execute(new ChangeEvent(PROFILE_CREATE, null, list));
            return list;
        }

        Profile p;

        if (ext.equals("zip")){
            path.extract(tempProfiles, null);
            String name = path.getFirstZipEntry().replace("/", "");
            var px = tempProfiles.to(name, "profile.json");
            p = GsonUtil.DEFAULT_GSON.fromJson(px.read(), Profile.class);
        }
        else if (ext.equals("json")){
            p = GsonUtil.DEFAULT_GSON.fromJson(path.read(), Profile.class);
            path.copy(tempProfiles.to(p.getName(), "profile.json"));
        }
        else
            return List.of();

        String first = p.getName();
        String gen = generateName(first);

        tempProfiles.to(first).move(profilesDir.to(gen));

        verifyProfileIcon(p);
        p.setName(gen).save();


        profiles.add(p);
        var l = List.of(p);
        handler.execute(new ChangeEvent(PROFILE_CREATE, null, l));
        return l;
    }

    /**
     * Import profile from the objects.
     * @param ps profile objects
     */
    public void importProfiles(List<Profile> ps){
        ps.forEach(p -> {
            String gen = generateName(p.getName());

            verifyProfileIcon(p);
            p.setName(gen).save();

            profiles.add(p);
        });
        handler.execute(new ChangeEvent(PROFILE_CREATE, null, ps));
    }

    public static boolean verifyProfileIcon(Profile p){
        if (p.getIcon() != null && p.getIcon().isBase64()){
            String name = UUID.randomUUID() + ".png";
            var path = Configurator.getConfig().getImagePath();
            try(var str = new ByteArrayInputStream(ImageCacheManager.decodeImage(p.getIcon().getIdentifier()));
                var fileStream = new FileOutputStream(path.to(name).toFile())) {
                fileStream.write(str.readAllBytes());

                p.setIcon(ImageEntity.fromLocal(name));
            } catch (IOException e) {
                p.setIcon(null);
            }

            return true;
        }

        return false;
    }

    public static String verifyProfileJsonIcon(Profile profile){
        var json = profile.getPath().to("profile.json");
        String read;
        if (profile.getIcon() != null && !profile.getIcon().isNetwork() && profile.getIcon().getUrl() == null){
            var p = profile.getIcon().getPath(Configurator.getConfig().getImagePath());
            var f = GsonUtil.DEFAULT_GSON.fromJson(json.read(), Profile.class);
            f.setIcon(p.exists() ? ImageEntity.fromBase64(ImageCacheManager.encodeImage(p)) : null);
            read = GsonUtil.DEFAULT_GSON.toJson(f);
        }
        else
            read = json.read();

        return read;
    }

    public String generateName(String name){
        if (profiles.stream().noneMatch(x -> x.getName().equals(name)))
            return name;

        int identifier = 0;
        while (true){
            int idf = ++identifier;
            if (profiles.stream().noneMatch(x -> x.getName().equals(name + idf)))
                break;
        }
        return name + identifier;
    }

    /**
     * Set profile from the name.
     * <br/>
     * Execute the profile handlers.
     * @param name name of the profile
     * @param set executor
     */
    public void setProfile(String name, Consumer<Profile> set){
        var profile = getProfile(name);
        setProfile(profile, set);
    }

    /**
     * Set profile.
     * <br/>
     * Execute the profile handlers.
     * @param profile the profile
     * @param set executor
     */
    private void setProfile(Profile profile, Consumer<Profile> set){
        try{
            String n = profile.getName();
            if (set != null)
                set.accept(profile);
            if (profile.getName() == null)
                return;
            profile.save();
            if (!profile.getName().equals(n)){
                profilesDir.to(n).move(profile.getPath());
            }

            handler.execute(new ChangeEvent(PROFILE_UPDATE, Profile.fromName(n), profile));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public void reload(){
        try{
            profiles = profilesDir.getFiles().stream().map(Profile::get).collect(Collectors.toList());
            profiles.removeIf(a -> a == null || a.isEmpty());
            handler.execute(new ChangeEvent(EventHandler.RELOAD, null, null));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public Profile copyProfile(Profile p){
        var p2 = Profile.empty().cloneFrom(p);
        p2.setName(generateName(p2.getName())).save();
        profiles.add(p2);
        handler.execute(new ChangeEvent(PROFILE_CREATE, null, List.of(p2)));
        return p2;
    }

    public Profile createProfile(String name) {
        try{
            if (profilesDir.getFiles().stream().anyMatch(x -> x.getName().equals(name)))
                return Profile.empty();

            var profile = Profile.get(profilesDir.to(StrUtil.pure(name)));
            profiles.add(profile);
            handler.execute(new ChangeEvent(PROFILE_CREATE, null, List.of(profile)));
            return profile;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return Profile.empty();
        }
    }

    public Profile createAndSetProfile(String name, Consumer<Profile> set){
        var profile = createProfile(name);
        if (profile.isEmpty())
            return getProfile(name);
        setProfile(profile, set);
        return profile;
    }

    public void deleteProfile(Profile p){
        profilesDir.to(p.getName()).delete();
        profiles.remove(p);

        handler.execute(new ChangeEvent(PROFILE_DELETE, p, null));
    }


}









