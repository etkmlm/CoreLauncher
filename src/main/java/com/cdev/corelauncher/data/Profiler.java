package com.cdev.corelauncher.data;

import com.cdev.corelauncher.data.entities.ChangeEvent;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.ui.utils.EventHandler;
import com.cdev.corelauncher.utils.entities.Path;

import java.util.List;
import java.util.stream.Collectors;


public class Profiler {
    private static Profiler instance;
    private Path profilesDir;
    private List<Profile> profiles;
    private EventHandler<ChangeEvent> handler;

    public Profiler() {
        profilesDir = Configurator.getConfig().getGamePath().to("profiles");

        handler = new EventHandler<>();

        Configurator.getConfigurator().getHandler().addHandler("profiler", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;
            profilesDir = (Path)a.getNewValue();

            reload();
        });

        instance = this;
    }

    public EventHandler<ChangeEvent> getHandler(){
        return handler;
    }

    public static Profiler getProfiler(){
        return instance;
    }

    public void moveProfiles(Path oldProfiles){
        oldProfiles.getFiles().forEach(x -> x.move(profilesDir, false));
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

    public Profiler reload(){
        profiles = profilesDir.getFiles().stream().map(Profile::get).collect(Collectors.toList());
        return this;
    }


    /**
     * Creates new profile.
     * @param name
     * Profile name
     * @return newly created profile,
     * null if there is an object with the same name
     */
    public Profile createProfile(String name) {
        if (profilesDir.getFiles().stream().anyMatch(x -> x.getName().equals(name)))
            return Profile.empty();

        var profile = Profile.get(profilesDir.to(name));
        profiles.add(profile);
        handler.execute(new ChangeEvent("profileCreate", null, profile, null));
        return profile;
    }

    public void deleteProfile(Profile p){
        profilesDir.to(p.getName()).delete();
        profiles.remove(p);

        handler.execute(new ChangeEvent("profileDelete", p, null, null));
    }


}









