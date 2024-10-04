package com.laeben.corelauncher.minecraft.wrapper.neoforge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.forge.installer.ForgeInstaller;
import com.laeben.corelauncher.minecraft.wrapper.neoforge.entity.NeoForgeVersion;
import com.laeben.corelauncher.util.GsonUtil;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class NeoForge extends Wrapper<NeoForgeVersion> {

    private static final String NEO_INDEX = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge";
    private static final String NEO_INSTALLER = "https://maven.neoforged.net/releases/net/neoforged/neoforge/%/neoforge-%-installer.jar";

    private static final List<NeoForgeVersion> cache = new ArrayList<>();

    private final Gson gson;

    public NeoForge(){
        gson = new Gson();
    }

    @Override
    public LoaderType getType() {
        return LoaderType.NEOFORGE;
    }

    @Override
    public NeoForgeVersion getVersionFromIdentifier(String identifier, String inherits) {
        if (inherits == null)
            inherits = "*";

        return identifier.toLowerCase().contains(getType().getIdentifier()) ? new NeoForgeVersion(inherits, identifier.split("-")[1]) : null;
    }

    @Override
    public NeoForgeVersion getVersion(String id, String wrId) {
        return getVersions(id).stream().filter(a -> a.getWrapperVersion().equals(wrId)).findFirst().orElse(new NeoForgeVersion(wrId));
    }

    @Override
    public List<NeoForgeVersion> getAllVersions() {
        if (!cache.isEmpty() && !disableCache)
            return cache;



        try {
            var all = gson.fromJson(NetUtil.urlToString(NEO_INDEX), JsonObject.class);
            cache.clear();
            cache.addAll(all.get("versions").getAsJsonArray().asList().stream().map(a -> new NeoForgeVersion(a.getAsString())).toList());
            Collections.reverse(cache);
        } catch (NoConnectionException ignored) {
            return getOfflineVersions();
        } catch (HttpException e) {
            Logger.getLogger().log(e);
        }

        return cache;
    }

    private String getNeoInstaller(NeoForgeVersion v){
        return NEO_INSTALLER.replace("%", v.getWrapperVersion());
    }

    @Override
    public List<NeoForgeVersion> getVersions(String versionId) {
        logState("acqVersionForge - " + versionId);

        return getAllVersions().stream().filter(a -> a.id.equals(versionId)).toList();
    }

    @Override
    public void install(NeoForgeVersion version) throws NoConnectionException, StopException, PerformException {
        var versionsPath = Configurator.getConfig().getGamePath().to("versions");
        var verPath = versionsPath.to(version.getJsonName());
        var verJsonPath = verPath.to(version.getJsonName() + ".json");

        Vanilla.getVanilla().install(version);

        if (verJsonPath.exists() && !disableCache)
            return;

        String installerUrl = getNeoInstaller(version);

        try{
            logState(".forge.state.neodownload");
            var path = Configurator.getConfig().getTemporaryFolder();
            path = NetUtil.download(installerUrl, path, true, false);

            if (stopRequested)
                throw new StopException();

            var target = Configurator.getConfig().getGamePath().toFile();

            logState(".forge.state.neoinstall");

            var profileInfo = generateProfileInfo(Path.begin(target.toPath()));
            boolean success = false;
            try(URLClassLoader loader = new URLClassLoader(new URL[]{path.toFile().toURI().toURL()})){
                for (var installer : ForgeInstaller.INSTALLERS){
                    try{
                        installer.install(loader, target, this::logState);
                        success = true;
                        break;
                    } catch (NoSuchMethodException | ClassNotFoundException ignored){

                    }
                    catch (InvocationTargetException t){
                        var c = t.getCause();

                        int x = 0;
                    }
                }
            }
            catch (Exception e){
                Logger.getLogger().logHyph("ERRNEOFORGE " + version.getWrapperVersion());
                Logger.getLogger().log(e);
                logState(".error.unknown");
            }

            path.delete();
            getGameDir().getFiles().forEach(x -> {
                if (x.getExtension() != null && x.getExtension().equals("jar"))
                    x.delete();
            });

            if (!success){
                throw new PerformException(".forge.error.unknownInstaller");
            }

            logState(".launch.state.finish");

            var read = GsonUtil.empty().fromJson(profileInfo.read(), JsonObject.class);
            var profiles = read.get("profiles").getAsJsonObject();
            var forge = profiles.get("neoforge");
            if (forge == null)
                forge = profiles.get("NeoForge");
            String name = forge.getAsJsonObject().get("lastVersionId").getAsString();
            if (!version.getJsonName().equals(name)){
                versionsPath.to(name, name + ".json").move(verPath.to(version.getJsonName() + ".json"));
                versionsPath.to(name).delete();
            }
        }
        catch (StopException | PerformException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }
}
