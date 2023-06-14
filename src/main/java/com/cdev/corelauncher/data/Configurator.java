package com.cdev.corelauncher.data;

import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.data.entities.ChangeEvent;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.ui.utils.EventHandler;
import com.cdev.corelauncher.utils.GsonUtils;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.util.Locale;

public class Configurator {
    private static Configurator instance;
    private final EventHandler<ChangeEvent> handler;

    private static Config defaultConfig;
    private static Config config;
    private final Path configFilePath;
    private static final Gson gson = GsonUtils.DEFAULT_GSON.newBuilder().registerTypeAdapter(Profile.class, new Profile.ProfileFactory()).create();


    public Configurator(Path configPath){
        configFilePath = configPath.to("config.json");

        defaultConfig = generateDefaultConfig();
        handler = new EventHandler<>();

        instance = this;
    }

    public static Config generateDefaultConfig() {
        var file = Config.class.getResourceAsStream("/com/cdev/corelauncher/data/config.json");
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

            config = gson.fromJson(read, Config.class);
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
        save();

        handler.execute(new ChangeEvent("gamePathChange", oldPath, path, null));
        return this;
    }

    public Configurator setLanguage(Locale l){
        var oldLang = config.getLanguage();
        config.setLanguage(l);
        Translator.getTranslator().setLanguage(l);
        save();

        handler.execute(new ChangeEvent("languageChange", oldLang, l, null));
        return this;
    }

    public static Configurator getConfigurator(){
        return instance;
    }

    private void save(Config c){
        String serialized = gson.toJson(c);
        configFilePath.write(serialized);
    }

    public static void save(){
        instance.save(config);
    }
}
