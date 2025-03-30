package com.laeben.corelauncher.api;

import com.laeben.core.util.events.ChangeEvent;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.api.entity.Config;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.core.entity.Path;
import com.google.gson.Gson;
import com.laeben.corelauncher.util.entity.LogType;

import java.io.InputStreamReader;
import java.util.Locale;

public class Configurator {
    public static final String BACKGROUND_CHANGE = "bgChange";
    public static final String LANGUAGE_CHANGE = "langChange";
    public static final String USER_CHANGE = "userChange";
    public static final String GAME_PATH_CHANGE = "gamePathChange";

    private static Configurator instance;
    private final EventHandler<ChangeEvent> handler;

    private static Config config;
    private static int configLoadIndex = 1;
    private final Path configFilePath;
    private static final Gson gson = GsonUtil.DEFAULT_GSON.newBuilder().
            registerTypeAdapter(Profile.class, new Profile.ProfileFieldFactory()).create();


    public Configurator(Path configPath){
        configFilePath = configPath.to("config.json");
        handler = new EventHandler<>();

        instance = this;
    }

    public static Gson getProfileGson(){
        return gson;
    }

    public static Config generateDefaultConfig() {
        try{
            var file = CoreLauncherFX.class.getResourceAsStream("data/config.json");
            if (file == null){
                Logger.getLogger().log(LogType.ERROR,"NOT FOUND DEFAULT CONFIG FILE IN: " + "data/config.json");
                return null;
            }
            return GsonUtil.DEFAULT_GSON.fromJson(new InputStreamReader(file), Config.class);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return null;
        }
    }

    public EventHandler<ChangeEvent> getHandler(){
        return handler;
    }

    public boolean reloadConfig(){
        if (configLoadIndex == 5){
            configLoadIndex = 1;
            return false;
        }
        configLoadIndex++;
        if (configFilePath.exists()){
            var read = configFilePath.read();
            Logger.getLogger().logDebug("Loading config from: " + read);
            config = gson.fromJson(read, Config.class);

            configLoadIndex = 1;
            return true;
        }
        else{
            return save(generateDefaultConfig()) && reloadConfig();
        }
    }

    public static Config getConfig(){
        return config;
    }

    public void setGamePath(Path path){
        var oldPath = config.getGamePath();
        config.setGamePath(path);
        save();

        handler.execute(new ChangeEvent(GAME_PATH_CHANGE, oldPath, path));
    }

    public void setDefaultAccount(Account account){
        config.setUser(account);
        save();

        handler.execute(new ChangeEvent(USER_CHANGE, null, account));
    }

    public void setCustomBackground(Path path){
        config.setBackgroundImage(path);
        save();

        handler.execute(new ChangeEvent(BACKGROUND_CHANGE, null, path));
    }

    public void setLanguage(Locale l){
        var oldLang = config.getLanguage();
        config.setLanguage(l);
        Translator.getTranslator().setLanguage(l);
        save();

        handler.execute(new ChangeEvent(LANGUAGE_CHANGE, oldLang, l));
    }

    public static Configurator getConfigurator(){
        return instance;
    }

    public void reset(){
        var path = config.getGamePath();
        var backup = config;
        config = generateDefaultConfig();
        if (config == null){
            config = backup;
            return;
        }
        config.setGamePath(path);
        save();

        handler.execute(new ChangeEvent(LANGUAGE_CHANGE, null, config.getLanguage()));
        handler.execute(new ChangeEvent(BACKGROUND_CHANGE, null, config.getBackgroundImage()));
        handler.execute(new ChangeEvent(USER_CHANGE, null, config.getUser()));
    }

    private boolean save(Config c){
        try{
            if (c == null)
                return false;
            String serialized = gson.toJson(c);
            Logger.getLogger().logDebug("Saving config file: " + c.getUser().getUsername() + "\n" + serialized);
            configFilePath.write(serialized);

            return configFilePath.exists();
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return false;
        }
    }

    public static boolean save(){
        return instance.save(config);
    }
}
