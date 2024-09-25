package com.laeben.corelauncher.minecraft.wrapper.forge;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.forge.entity.FArtifact;
import com.laeben.corelauncher.minecraft.wrapper.forge.entity.ForgeVersion;
import com.laeben.corelauncher.minecraft.wrapper.forge.installer.ForgeInstaller;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.core.entity.Path;
import com.google.gson.JsonObject;
import org.jsoup.HttpStatusException;

import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;

public class Forge extends Wrapper<ForgeVersion> {
    private static final String FORGE_INDEX = "https://files.minecraftforge.net/net/minecraftforge/forge/index_";
    private static final Map<String, List<ForgeVersion>> cache = new HashMap<>();

    public Forge(){

    }

    @Override
    public ForgeVersion getVersion(String id, String wrId){
        return getVersions(id).stream().filter(x -> x.getWrapperVersion().equals(wrId)).findFirst().orElse(new ForgeVersion(id));
    }

    @Override
    public List<ForgeVersion> getAllVersions() {
        return null;
    }

    @Override
    public ForgeVersion getVersionFromIdentifier(String identifier, String inherits){
        if (inherits == null)
            inherits = "*";
        return identifier.toLowerCase().contains("-" + getType().getIdentifier()) ? new ForgeVersion(inherits, identifier.split("-")[2]) : null;
    }

    @Override
    public List<ForgeVersion> getVersions(String versionId) {
        logState("acqVersionForge - " + versionId);

        if (cache.containsKey(versionId) && !disableCache)
            return cache.get(versionId);

        cache.remove(versionId);

        String webPage = FORGE_INDEX + versionId + ".html";
        var versions = new ArrayList<ForgeVersion>();

        try{
            var doc = NetUtil.getDocumentFromUrl(webPage);

            var table = doc.selectXpath("//table[@class='download-list'][1]/tbody/tr");

            for(var element : table){
                var s = element.getElementsByClass("download-version").get(0).text().split(" ")[0];
                ForgeVersion forge = new ForgeVersion(versionId)
                        .setWrapperVersion(s);

                boolean latest = !element.getElementsByClass("promo-latest").isEmpty();
                boolean recommended = !element.getElementsByClass("promo-recommended").isEmpty();

                forge.forgeVersionType = latest ? ForgeVersion.ForgeVersionType.LATEST : recommended ? ForgeVersion.ForgeVersionType.RECOMMENDED : ForgeVersion.ForgeVersionType.NORMAL;
                String date = element.getElementsByClass("download-time").get(0).attr("title");
                forge.releaseTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date);

                //var files = element.selectXpath(".//ul[@class='download-links']/li");
                var files = element.getElementsByClass("download-links").stream().flatMap(x -> x.getElementsByTag("li").stream()).toList();
                forge.fArtifacts = files.stream().map(file -> {
                    try{
                        var a = file.getElementsByTag("a").get(0);
                        String url = a.attr("href");
                        String title = a.text();

                        return new FArtifact(title, url);
                    }
                    catch (Exception e){
                        return null;
                    }
                }).filter(Objects::nonNull).toList();

                versions.add(forge);
            }

        }
        catch (NoConnectionException e){
            return getOfflineVersions().stream().filter(x -> x.checkId(versionId)).toList();
        }
        catch (HttpStatusException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
        cache.put(versionId, versions);
        return versions;
    }

    @Override
    public void install(ForgeVersion version) throws NoConnectionException, StopException, PerformException {
        var versionsPath = Configurator.getConfig().getGamePath().to("versions");
        var verPath = versionsPath.to(version.getJsonName());
        var verJsonPath = verPath.to(version.getJsonName() + ".json");

        Vanilla.getVanilla().install(version);

        if (verJsonPath.exists() && !disableCache)
            return;

        var art = version.fArtifacts.stream().filter(x -> x.type == FArtifact.Type.INSTALLER).findFirst().orElse(null);
        if (art == null)
            return;

        try{
            logState(".forge.state.download");
            var path = Configurator.getConfig().getTemporaryFolder();
            path = NetUtil.download(art.getUrl(), path, true, false);

            if (stopRequested)
                throw new StopException();

            var target = Configurator.getConfig().getGamePath().toFile();

            logState(".forge.state.install");

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
                }
            }
            catch (Exception e){
                Logger.getLogger().logHyph("ERRFORGE " + version.getWrapperVersion());
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
            var forge = profiles.get("forge");
            if (forge == null)
                forge = profiles.get("Forge");
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

        //profileInfo.delete();
    }


    @Override
    public LoaderType getType() {
        return LoaderType.FORGE;
    }
}
