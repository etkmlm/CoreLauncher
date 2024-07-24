package com.laeben.corelauncher.minecraft.wrapper;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.core.entity.Path;

import java.util.List;

public class Custom extends Wrapper<Version> {
    @Override
    public Version getVersionFromIdentifier(String identifier, String inherits) {
        return null;
    }

    @Override
    public Version getVersion(String id, String wrId) {
        try{
            return GsonUtil.DEFAULT_GSON.fromJson(Configurator.getConfig().getGamePath().to("versions", wrId, wrId + ".json").read(), Version.class);
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
    public void install(Version v) throws NoConnectionException, StopException {
        logState("prepare" + v.id);

        downloadLibraries(v);
        downloadAssets(v);
    }


    @Override
    public LoaderType getType() {
        return LoaderType.CUSTOM;
    }
}
