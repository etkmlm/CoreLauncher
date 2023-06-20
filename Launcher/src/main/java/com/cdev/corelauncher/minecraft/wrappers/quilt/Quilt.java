package com.cdev.corelauncher.minecraft.wrappers.quilt;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.minecraft.entities.Version;
import com.cdev.corelauncher.minecraft.wrappers.Vanilla;
import com.cdev.corelauncher.minecraft.wrappers.fabric.Fabric;
import com.cdev.corelauncher.minecraft.wrappers.fabric.entities.FabricVersion;
import com.cdev.corelauncher.utils.GsonUtils;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.entities.NoConnectionException;

public class Quilt extends Fabric {

    private static final String BASE_URL = "https://meta.quiltmc.org/v3/";

    @Override
    public String getBaseUrl(){
        return BASE_URL;
    }

    @Override
    public String getIdentifier(){
        return "quilt";
    }

    @Override
    public String getInstaller(){
        return "https://quiltmc.org/api/v1/download-latest-installer/java-universal";
    }

    @Override
    public void install(FabricVersion v){
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
            var path = NetUtils.download(installer, temp.to("quiltinstaller.jar"), false, null);

            logState(".quilt.state.install");
            try{
                var process = new ProcessBuilder()
                        .command(JavaMan.getDefault().getExecutable().toString(),
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

            v = GsonUtils.DEFAULT_GSON.fromJson(jsonPath.read(), FabricVersion.class).setWrapperVersion(v.getWrapperVersion());
            downloadLibraries(v);

        }
        catch (NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

    }
}
