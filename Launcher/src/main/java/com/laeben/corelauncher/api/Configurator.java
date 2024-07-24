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

import java.io.InputStreamReader;
import java.util.Locale;

public class Configurator {
    private static Configurator instance;
    private final EventHandler<ChangeEvent> handler;

    private static Config config;
    private final Path configFilePath;
    private static final Gson gson = GsonUtil.DEFAULT_GSON.newBuilder().registerTypeAdapter(Profile.class, new Profile.ProfileFactory()).create();


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

            assert file != null;
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

    public Configurator reloadConfig(){
        if (configFilePath.exists()){

            var read = configFilePath.read();

            config = gson.fromJson(read, Config.class);
        }
        else{
            save(generateDefaultConfig());
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

    public void setDefaultAccount(Account account){
        config.setUser(account);
        save();

        handler.execute(new ChangeEvent("userChange", null, account));
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

        handler.execute(new ChangeEvent("languageChange", null, config.getLanguage()));
        handler.execute(new ChangeEvent("bgChange", null, config.getBackgroundImage()));
        handler.execute(new ChangeEvent("userChange", null, config.getUser()));
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
