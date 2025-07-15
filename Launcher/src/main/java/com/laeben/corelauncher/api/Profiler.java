package com.laeben.corelauncher.api;

import com.laeben.core.entity.Path;
import com.laeben.core.util.events.ChangeEvent;
import com.laeben.corelauncher.api.entity.ImageEntity;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.shortcut.Shortcut;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.minecraft.loader.Vanilla;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.util.entity.LogType;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
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


    /**
     * Create a shortcut of your profile to desired destination. Apply the profile image if it is possible.
     * @param destination shortcut path
     */
    public static void createShortcut(Profile p, Path destination) throws URISyntaxException {
        var javaPath = Path.begin(OSUtil.getJavaFile(OSUtil.getRunningJavaDir().toString(), true));

        Path iconPath;

        if (OS.getSystemOS() != OS.WINDOWS && p.getIcon() != null && !p.getIcon().isEmpty() && !p.getIcon().isNetwork() && !p.getIcon().isEmbedded() && !p.getIcon().isBase64()){
            iconPath = p.getIcon().getPath(Configurator.getConfig().getImagePath());
        }
        else {
            String icon = OS.getSystemOS() == OS.WINDOWS ? "shortcut.ico" : "shortcut.png";
            iconPath = Configurator.getConfig().getLauncherPath().to(icon);

            boolean t = iconPath.exists();

            if (!iconPath.exists())
                t = ImageUtil.extractLocalImage("shortcut/" + icon, iconPath);

            if (!t)
                iconPath = null;
        }

        if (destination.exists())
            destination.delete();

        var targetPath = Path.begin(new File(Profiler.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath());

        var args = String.format("-jar \"%s\" --launch %s", targetPath, p.getName());

        Shortcut.create(destination, javaPath, targetPath.parent(), iconPath, args, OS.getSystemOS());
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

    public static void setResourceDisabled(Profile p, CResource resource, boolean disabled){
        if (resource.getType() == ResourceType.MODPACK || resource.getType() == ResourceType.WORLD) // not for these :)
            return;

        resource.disabled = disabled;
        var path = p.getPath().to(resource.getType().getStoringFolder(), resource.fileName);
        var disabledPath = p.getPath().to(resource.getType().getStoringFolder(), resource.fileName + ".dis");
        if (disabled){
            if (path.exists())
                path.move(disabledPath);
        }
        else{
            if (disabledPath.exists())
                disabledPath.move(path);
        }
        UI.runAsync(p::save);
    }

    private Profile importFromBackup(Path p){
        var profile = Profile.PROFILE_GSON.fromJson(p.read(), Profile.class);
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
    public List<Profile> importFromPath(Path path, Predicate<Profile> determineOverwrite){
        var ext = path.getExtension();
        var tempProfiles = Configurator.getConfig().getTemporaryFolder();

        if (path.isDirectory()){ // backup
            var pxn = path.to("profile.json");

            var list = new ArrayList<Profile>();

            if (pxn.exists())
                list.add(importFromBackup(pxn)); // single backup
            else{
                for (var i : path.getFiles()){ // multiple backup
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
            p = Profile.PROFILE_GSON.fromJson(px.read(), Profile.class);
        }
        else if (ext.equals("json")){
            p = Profile.PROFILE_GSON.fromJson(path.read(), Profile.class);
            path.copy(tempProfiles.to(p.getName(), "profile.json"));
        }
        else
            return List.of();

        String first = p.getName();
        String gen = generateName(first);

        if (!first.equals(gen) && determineOverwrite != null && determineOverwrite.test(p)){
            setProfile(first, a -> a.copyModdingFrom(p));
            tempProfiles.to(first).delete();
            return List.of(p);
        }

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
    public void importProfiles(List<Profile> ps, Predicate<Profile> determineOverwrite){
        ps.forEach(p -> {
            String gen = generateName(p.getName());

            if (!p.getName().equals(gen) && determineOverwrite != null && determineOverwrite.test(p)){
                setProfile(p.getName(), a -> a.copyModdingFrom(p));
                return;
            }

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
            var f = Profile.PROFILE_GSON.fromJson(json.read(), Profile.class);
            f.setIcon(p.exists() ? ImageEntity.fromBase64(ImageCacheManager.encodeImage(p)) : null);
            read = Profile.PROFILE_GSON.toJson(f);
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

            // swapped 306-308 and 309, need to move the profile contents before saving
            if (!profile.getName().equals(n)){
                profilesDir.to(n).move(profile.getPath());
            }
            profile.save();

            handler.execute(new ChangeEvent(PROFILE_UPDATE, Profile.fromName(n), profile));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public void reload(){
        try{
            profiles = profilesDir.getFiles().stream().map(Profile::fromFolder).collect(Collectors.toList());
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

            var profile = Profile.fromFolder(profilesDir.to(StrUtil.pure(name)));
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

    public Profile generateDefaultProfile(){
        var release = Vanilla.getVanilla().getLatestRelease();
        if (release == null)
            Logger.getLogger().log(LogType.WARN, "Latest release is null!");
        return createAndSetProfile(Translator.translate("profile.defaultName"), a -> {
           a.setVersionId(release == null ? "1.21" : release);
        });
    }

}









