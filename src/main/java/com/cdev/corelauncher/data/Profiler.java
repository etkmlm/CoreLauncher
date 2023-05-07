package com.cdev.corelauncher.data;

import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.utils.entities.Path;

import java.util.List;


public class Profiler {

    private final Path profilesDir;
    private List<Profile> profiles;

    public Profiler() {
        profilesDir = Configurator.getConfig().getGamePath().to("profiles");
        profiles = profilesDir.getFiles().stream().map(Profile::new).toList();


    }

    public List<Profile> getAllProfiles(){
        return profiles;
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
            return null;

        return new Profile(profilesDir.to(name));
    }

    public void deleteProfile(Profile p){
        profilesDir.to(p.getName()).delete();
    }


}









