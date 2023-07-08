package com.laeben.corelauncher.data;

import com.laeben.core.util.events.ChangeEvent;
import com.laeben.corelauncher.data.entities.Config;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.utils.EventHandler;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.core.entity.Path;
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
        try{
            var file = Config.class.getResourceAsStream("/com/laeben/corelauncher/data/config.json");

            return GsonUtils.DEFAULT_GSON.fromJson(new InputStreamReader(file), Config.class);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return null;
        }
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

    public void setGamePath(Path path){
        var oldPath = config.getGamePath();
        config.setGamePath(path);
        save();

        handler.execute(new ChangeEvent("gamePathChange", oldPath, path));
    }

    public void setCustomBackground(Path path){
        config.setBackgroundImage(path);
        save();

        handler.execute(new ChangeEvent("bgChange", null, path));
    }

    public void setLanguage(Locale l){
        var oldLang = config.getLanguage();
        config.setLanguage(l);
        Translator.getTranslator().setLanguage(l);
        save();

        handler.execute(new ChangeEvent("languageChange", oldLang, l));
    }

    public static Configurator getConfigurator(){
        return instance;
    }

    private void save(Config c){
        try{
            String serialized = gson.toJson(c);
            configFilePath.write(serialized);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public static void save(){
        instance.save(config);
    }
}
