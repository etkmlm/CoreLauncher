package com.cdev.corelauncher.minecraft.wrappers;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.minecraft.entities.Version;
import com.cdev.corelauncher.utils.GsonUtils;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.Path;
import javafx.scene.layout.Pane;

import java.util.List;

public class Custom extends Wrapper<Version> {

    @Override
    public String getIdentifier() {
        return "custom";
    }

    @Override
    public Version getVersionFromIdentifier(String identifier, String inherits) {
        return null;
    }

    @Override
    public Version getVersion(String id, String wrId) {
        try{
            return GsonUtils.DEFAULT_GSON.fromJson(Configurator.getConfig().getGamePath().to("versions", wrId, wrId + ".json").read(), Version.class);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return new Version(id);
        }

    }

    public Path getPath(String wrId){
        return Configurator.getConfig().getGamePath().to("versions", wrId);
    }

    @Override
    public List<Version> getAllVersions() {
        return null;
    }

    @Override
    public List<Version> getVersions(String id) {
        return null;
    }

    @Override
    public void install(Version v) {
        logState("prepare" + v.id);

        downloadLibraries(v);
        downloadAssets(v);
    }
}
