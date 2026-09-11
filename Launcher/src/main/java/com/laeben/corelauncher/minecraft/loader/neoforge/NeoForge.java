package com.laeben.corelauncher.minecraft.loader.neoforge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.network.Network;
import com.laeben.core.network.entity.NetworkToken;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.loader.entity.RedownloadSettings;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.loader.Vanilla;
import com.laeben.corelauncher.minecraft.loader.forge.installer.ForgeInstaller;
import com.laeben.corelauncher.minecraft.loader.neoforge.entity.NeoForgeVersion;
import com.laeben.corelauncher.minecraft.token.VersionToken;
import com.laeben.corelauncher.util.GsonUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class NeoForge extends Loader<NeoForgeVersion> {

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
    public NeoForgeVersion getVersion(String id, String wrId, RedownloadSettings redownloadSettings) {
        return getVersions(id, redownloadSettings).stream().filter(a -> a.getLoaderVersion().equals(wrId)).findFirst().orElse(null);
    }

    @Override
    public List<NeoForgeVersion> getAllVersions(RedownloadSettings redownloadSettings) {
        if (!cache.isEmpty() && !redownloadSettings.hasClient())
            return cache;

        try {
            var all = gson.fromJson(Network.urlToString(NEO_INDEX), JsonObject.class);
            cache.clear();
            cache.addAll(all.get("versions").getAsJsonArray().asList().stream().map(a -> new NeoForgeVersion(a.getAsString())).toList());
            Collections.reverse(cache);
        } catch (NoConnectionException | StopException ignored) {
            return getOfflineVersions();
        } catch (HttpException | IOException e) {
            Logger.getLogger().log(e);
        }

        return cache;
    }

    private String getNeoInstaller(NeoForgeVersion v){
        return NEO_INSTALLER.replace("%", v.getLoaderVersion());
    }

    @Override
    public List<NeoForgeVersion> getVersions(String versionId, RedownloadSettings redownloadSettings) {
        logState("acqVersionForge - " + versionId);

        return getAllVersions(redownloadSettings).stream().filter(a -> a.id.equals(versionId)).toList();
    }

    @Override
    public void install(VersionToken<NeoForgeVersion> token) throws NoConnectionException, StopException, PerformException {
        final NeoForgeVersion version = token.getVersion();

        var versionsPath = Configurator.getConfig().getGamePath().to("versions");
        var verPath = versionsPath.to(version.getJsonName());
        var verJsonPath = verPath.to(version.getJsonName() + ".json");

        Vanilla.getVanilla().install(token);

        if (verJsonPath.exists() && !token.getRedownloadSettings().hasClient())
            return;

        String installerUrl = getNeoInstaller(version);

        try{
            logState(".forge.state.neodownload");
            var path = Configurator.getConfig().getTemporaryFolder();
            path = Network.download(NetworkToken.create(installerUrl, path, true).syncWith(token));

            if (token.shouldStop())
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
                Logger.getLogger().log("ERRNEOFORGE " + version.getLoaderVersion(), e);
                logState(UNKNOWN_ERROR);
            }

            path.delete();
            getGameDir().getFiles().forEach(x -> {
                if (x.getExtension() != null && x.getExtension().equals("jar"))
                    x.delete();
            });

            if (!success){
                throw new PerformException(".forge.error.unknownInstaller");
            }

            logState(LAUNCH_FINISH);

            var read = GsonUtil.EMPTY_GSON.fromJson(profileInfo.read(), JsonObject.class);
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
        catch (StopException | NoConnectionException | PerformException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }
}
