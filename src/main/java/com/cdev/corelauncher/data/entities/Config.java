package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.utils.JavaManager;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.OSUtils;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;

public class Config {
    private static final Path DEFAULT_GAME_PATH = new Path(OSUtils.getAppFolder());

    private Account user;
    private Path gamePath;
    private Language language;
    private Background background;
    private String defaultJVMArgs;
    private String lastSelectedProfile;
    private Integer defaultMinRAM;
    private Integer defaultMaxRAM;
    private Boolean showOldReleases;
    private Boolean showSnapshots;

    public Path getGamePath(){
        return gamePath == null ? DEFAULT_GAME_PATH : gamePath;
    }

    public void setGamePath(Path gamePath){
        this.gamePath = gamePath;
    }

}
