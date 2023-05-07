package com.cdev.corelauncher.data;

import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.utils.GsonUtils;
import com.cdev.corelauncher.utils.entities.Path;

import java.io.InputStreamReader;

public class Configurator {
    private static Configurator instance;

    private static Config defaultConfig;
    private static Config config;
    private final Path configFilePath;


    public Configurator(Path configPath){
        configFilePath = configPath.to("config.json");

        defaultConfig = generateDefaultConfig();

        instance = this;
    }

    public static Config generateDefaultConfig() {
        var file = Config.class.getResourceAsStream("/com/cdev/corelauncher/json/config.json");
        if (file == null)
            throw new RuntimeException("Default config file can't found.");

        return GsonUtils.DEFAULT_GSON.fromJson(new InputStreamReader(file), Config.class);
    }

    public Configurator reloadConfig(){
        if (configFilePath.exists()){

            var read = configFilePath.read();

            config = GsonUtils.DEFAULT_GSON.fromJson(read, Config.class);
        }
        else{
            save(defaultConfig);
            return reloadConfig();
        }

        return this;
    }

    public static Config getConfig(){
        return config;
    }

    public static Configurator getConfigurator(){
        return instance;
    }

    private void save(Config c){
        String serialized = GsonUtils.DEFAULT_GSON.toJson(c);
        configFilePath.write(serialized);
    }

    public void save(){
        save(config);
    }
}
