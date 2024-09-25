package com.laeben.corelauncher.minecraft.wrapper.quilt;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.fabric.Fabric;
import com.laeben.corelauncher.minecraft.wrapper.fabric.entity.BaseFabricVersion;
import com.laeben.corelauncher.minecraft.wrapper.fabric.entity.FabricVersion;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.util.JavaManager;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;

public class Quilt extends Fabric<QuiltVersion> {

    private static final String BASE_URL = "https://meta.quiltmc.org/v3/";

    @Override
    public String getBaseUrl(){
        return BASE_URL;
    }

    @Override
    public String getInstaller(){
        return "https://quiltmc.org/api/v1/download-latest-installer/java-universal";
    }

    @Override
    protected QuiltVersion getFabricVersion(){
        return new QuiltVersion();
    }

    @Override
    protected QuiltVersion getFabricVersion(String id, String wrId){
        return new QuiltVersion(id, wrId);
    }

    @Override
    public void install(QuiltVersion v) throws NoConnectionException, StopException {
        Vanilla.getVanilla().install(v);

        var gameDir = Configurator.getConfig().getGamePath();
        String jsonName = v.getJsonName();
        var temp = Configurator.getConfig().getTemporaryFolder();
        var jsonPath = gameDir.to("versions", jsonName, jsonName + ".json");
        var clientPath = gameDir.to("versions", jsonName, jsonName + ".jar");
        if (clientPath.exists() && !disableCache)
            return;

        try{
            logState(".quilt.state.download");
            String installer = getInstaller();
            var path = NetUtil.download(installer, temp.to("quiltinstaller.jar"), false, false);

            if (stopRequested)
                throw new StopException();

            logState(".quilt.state.install");
            try{
                var process = new ProcessBuilder()
                        .command(JavaManager.getDefault().getExecutable().toString(),
                                "-jar", path.toString(),
                                "install", "client", v.id, v.getWrapperVersion(),
                                "--install-dir=" + gameDir,
                                "--no-profile")
                        .inheritIO()
                        .start();
                process.waitFor();
            }
            catch (Exception e){
                Logger.getLogger().logHyph("ERRQUILT " + v.getWrapperVersion());
                Logger.getLogger().log(e);
                logState(".error.unknown");
            }

            logState(".launch.state.finish");

            path.delete();

            v = GsonUtil.DEFAULT_GSON.fromJson(jsonPath.read(), FabricVersion.class).setWrapperVersion(v.getWrapperVersion());
            downloadLibraries(v);

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

    }


    @Override
    public LoaderType getType() {
        return LoaderType.QUILT;
    }
}
