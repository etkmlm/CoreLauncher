package com.cdev.corelauncher.data;

import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.data.entities.ChangeEvent;
import com.cdev.corelauncher.ui.utils.EventHandler;
import com.cdev.corelauncher.utils.GsonUtils;
import com.cdev.corelauncher.utils.entities.Path;

import java.io.InputStreamReader;

public class Configurator {
    private static Configurator instance;
    private final EventHandler<ChangeEvent> handler;

    private static Config defaultConfig;
    private static Config config;
    private final Path configFilePath;


    public Configurator(Path configPath){
        configFilePath = configPath.to("config.json");

        defaultConfig = generateDefaultConfig();
        handler = new EventHandler<>();

        instance = this;
    }

    public static Config generateDefaultConfig() {
        var file = Config.class.getResourceAsStream("/com/cdev/corelauncher/json/config.json");
        if (file == null)
            throw new RuntimeException("Default config file can't found.");

        return GsonUtils.DEFAULT_GSON.fromJson(new InputStreamReader(file), Config.class);
    }

    public EventHandler<ChangeEvent> getHandler(){
        return handler;
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

    public Configurator setGamePath(Path path){
        var oldPath = config.getGamePath();
        config.setGamePath(path);
        handler.execute(new ChangeEvent("gamePathChange", oldPath, path, null));

        return this;
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
