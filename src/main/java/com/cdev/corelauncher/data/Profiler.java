package com.cdev.corelauncher.data;

import com.cdev.corelauncher.data.entities.ChangeEvent;
import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.ui.utils.EventHandler;
import com.cdev.corelauncher.utils.entities.Path;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class Profiler {
    private static Profiler instance;
    private Path profilesDir;
    private List<Profile> profiles;
    private EventHandler<ChangeEvent> handler;
    private Profile selectedProfile;

    public Profiler() {
        profilesDir = gameDirToProfilesDir(Configurator.getConfig().getGamePath());

        handler = new EventHandler<>();

        Configurator.getConfigurator().getHandler().addHandler("profiler", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;
            profilesDir = gameDirToProfilesDir((Path) a.getNewValue());

            reload();
        });

        instance = this;
    }

    private static Path gameDirToProfilesDir(Path gameDir){
        return gameDir.to("launcher", "profiles");
    }

    public EventHandler<ChangeEvent> getHandler(){
        return handler;
    }

    public static Profiler getProfiler(){
        return instance;
    }

    public void moveProfiles(Path oldGamePath){
        oldGamePath.to("launcher").to("profiles").getFiles().forEach(x -> x.copy(profilesDir));
    }

    public Path getProfilesDir(){
        return profilesDir;
    }

    public List<Profile> getAllProfiles(){
        return profiles;
    }

    public Profile getProfile(String name){
        return profiles.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(Profile.empty());
    }

    public Profile setProfile(String name, Consumer<Profile> set){
        var profile = getProfile(name);
        return setProfile(profile, set);
    }

    private Profile setProfile(Profile profile, Consumer<Profile> set){
        set.accept(profile);
        profile.save();
        handler.execute(new ChangeEvent("profileUpdate", null, profile, null));
        return profile;
    }

    public void reload(){
        profiles = profilesDir.getFiles().stream().map(Profile::get).collect(Collectors.toList());
        handler.execute(new ChangeEvent("reload", null, null, null));
    }

    public Profile createProfile(String name) {
        if (profilesDir.getFiles().stream().anyMatch(x -> x.getName().equals(name)))
            return Profile.empty();

        var profile = Profile.get(profilesDir.to(name));
        profiles.add(profile);
        handler.execute(new ChangeEvent("profileCreate", null, profile, null));
        return profile;
    }

    public Profile createAndSetProfile(String name, Consumer<Profile> set){
        return setProfile(createProfile(name), set);
    }

    public void deleteProfile(Profile p){
        profilesDir.to(p.getName()).delete();
        profiles.remove(p);

        handler.execute(new ChangeEvent("profileDelete", p, null, null));
    }


}









