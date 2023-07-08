package com.laeben.corelauncher.data;

import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.utils.EventHandler;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.core.entity.Path;
import com.laeben.core.util.events.ChangeEvent;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Profiler {
    private static Profiler instance;
    private Path profilesDir;
    private List<Profile> profiles;
    private final EventHandler<ChangeEvent> handler;

    public Profiler() {
        profilesDir = profilesDir();

        handler = new EventHandler<>();

        Configurator.getConfigurator().getHandler().addHandler("profiler", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;
            profilesDir = profilesDir();

            reload();
        });

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

    public void moveProfiles(Path oldGamePath){
        try{
            oldGamePath.to("launcher").to("profiles").getFiles().forEach(x -> x.copy(profilesDir.to(x.getName())));
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

    public static void backup(Profile p, Path to){
        var path = p.getPath();
        path.zip(to);
    }

    public void importProfile(Path path){
        var ext = path.getExtension();
        var tempProfiles = Configurator.getConfig().getTemporaryFolder();
        Profile p;
        if (ext.equals("zip")){
            path.extract(tempProfiles, null);
            String name = path.getZipFirstFolder().replace("/", "");
            var px = tempProfiles.to(name, "profile.json");
            p = GsonUtils.DEFAULT_GSON.fromJson(px.read(), Profile.class);
        }
        else if (ext.equals("json")){
            p = GsonUtils.DEFAULT_GSON.fromJson(path.read(), Profile.class);
            path.copy(tempProfiles.to(p.getName(), "profile.json"));
        }
        else
            return;

        if (profiles.stream().anyMatch(x -> x.getName().equals(p.getName()))){
            String first = p.getName();
            int identifier = 1;
            do{
                p.rename(first + identifier++);
            }while (profiles.stream().anyMatch(x -> x.getName().equals(p.getName())));
            tempProfiles.to(first).move(profilesDir.to(p.save().getName()));
        }
        else
            tempProfiles.to(p.getName()).move(profilesDir().to(p.getName()));

        profiles.add(p);
        handler.execute(new ChangeEvent("profileCreate", null, p));
    }

    public Profile setProfile(String name, Consumer<Profile> set){
        var profile = getProfile(name);
        return setProfile(profile, set);
    }

    private Profile setProfile(Profile profile, Consumer<Profile> set){
        try{
            String n = profile.getName();
            if (set != null)
                set.accept(profile);
            if (profile.getName() == null)
                return profile;
            profile.save();
            if (!profile.getName().equals(n)){
                profilesDir.to(n).move(profile.getPath());
            }

            handler.execute(new ChangeEvent("profileUpdate", null, profile));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
        return profile;
    }

    public void reload(){
        try{
            profiles = profilesDir.getFiles().stream().map(Profile::get).collect(Collectors.toList());
            handler.execute(new ChangeEvent("reload", null, null));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public Profile createProfile(String name) {
        try{
            if (profilesDir.getFiles().stream().anyMatch(x -> x.getName().equals(name)))
                return Profile.empty();

            var profile = Profile.get(profilesDir.to(name));
            profiles.add(profile);
            handler.execute(new ChangeEvent("profileCreate", null, profile));
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
        return setProfile(profile, set);
    }

    public void deleteProfile(Profile p){
        profilesDir.to(p.getName()).delete();
        profiles.remove(p);

        handler.execute(new ChangeEvent("profileDelete", p, null));
    }


}









