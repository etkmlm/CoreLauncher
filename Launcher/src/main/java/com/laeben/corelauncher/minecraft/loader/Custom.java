package com.laeben.corelauncher.minecraft.loader;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.Launcher;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.loader.entity.RedownloadSettings;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.token.VersionToken;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.core.entity.Path;

import java.util.List;

public class Custom extends Loader<Version> {
    @Override
    public Version getVersionFromIdentifier(String identifier, String inherits) {
        return null;
    }

    @Override
    public Version getVersion(String id, String wrId, RedownloadSettings redownloadSettings) {
        try{
            return GsonUtil.DEFAULT_GSON.fromJson(Configurator.getConfig().getGamePath().to("versions", wrId, wrId + ".json").read(), Version.class);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return null;
        }

    }

    public Path getPath(String wrId){
        return Configurator.getConfig().getGamePath().to("versions", wrId);
    }

    @Override
    public List<Version> getAllVersions(RedownloadSettings redownloadSettings) {
        return null;
    }

    @Override
    public List<Version> getVersions(String id, RedownloadSettings redownloadSettings) {
        return null;
    }

    @Override
    public void install(VersionToken token) throws NoConnectionException, StopException {
        logState(Launcher.PREPARE + token.getVersion().id);

        downloadLibraries(token);
        downloadAssets(token);
    }


    @Override
    public LoaderType getType() {
        return LoaderType.CUSTOM;
    }
}
