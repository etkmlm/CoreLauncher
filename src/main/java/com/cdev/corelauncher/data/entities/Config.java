package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.utils.OSUtils;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;

public class Config {

    private static final Path DEFAULT_GAME_PATH = new Path(OSUtils.getAppFolder());
    private final Path configPath;

    private Path gamePath;


    public Config(Path configPath){
        this.configPath = configPath;
    }

    public Path getGamePath(){
        return gamePath == null ? DEFAULT_GAME_PATH : gamePath;
    }

    public Config setGamePath(Path gamePath){
        this.gamePath = gamePath;

        return this;
    }

    public static Config getConfig(){
        return null;
    }

    public void save(){
        String serialized = new Gson().toJson(this);
        configPath.write(serialized);
    }

}
