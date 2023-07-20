package com.laeben.corelauncher.minecraft.wrappers;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.entities.Version;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.CurseWrapper;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.core.entity.Path;

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
    public void install(Version v) throws NoConnectionException, StopException {
        logState("prepare" + v.id);

        downloadLibraries(v);
        downloadAssets(v);
    }


    @Override
    public CurseWrapper.Type getType() {
        return CurseWrapper.Type.ANY;
    }
}
