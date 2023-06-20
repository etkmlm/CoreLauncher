package com.cdev.corelauncher.minecraft.wrappers.forge;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.minecraft.wrappers.Vanilla;
import com.cdev.corelauncher.minecraft.wrappers.fabric.entities.FabricVersion;
import com.cdev.corelauncher.minecraft.wrappers.forge.entities.FArtifact;
import com.cdev.corelauncher.minecraft.wrappers.forge.entities.ForgeVersion;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.NoConnectionException;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;

public class Forge extends Wrapper<ForgeVersion> {
    private static final String FORGE_INDEX = "https://files.minecraftforge.net/net/minecraftforge/forge/index_";
    private static final Map<String, List<ForgeVersion>> cache = new HashMap<>();

    public Forge(){

    }

    @Override
    public String getIdentifier() {
        return "forge";
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
        return identifier.toLowerCase().contains("forge") ? new ForgeVersion(inherits, identifier.split("-")[2]) : null;
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
            Document doc = Jsoup.connect(webPage).get();

            var table = doc.selectXpath("//table[@class='download-list'][1]/tbody/tr");

            for(var element : table){
                ForgeVersion forge = new ForgeVersion(versionId)
                        .setWrapperVersion(element.getElementsByClass("download-version").get(0).text().split(" ")[0]);

                boolean latest = element.getElementsByClass("promo-latest").size() != 0;
                boolean recommended = element.getElementsByClass("promo-recommended").size() != 0;

                forge.forgeVersionType = latest ? ForgeVersion.ForgeVersionType.LATEST : recommended ? ForgeVersion.ForgeVersionType.RECOMMENDED : ForgeVersion.ForgeVersionType.NORMAL;
                String date = element.getElementsByClass("download-time").get(0).attr("title");
                forge.releaseTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date);

                var files = element.selectXpath(".//ul[@class='download-links']/li");
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
        catch (UnknownHostException e){
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
    public void install(ForgeVersion version){
        var versionsPath = Configurator.getConfig().getGamePath().to("versions");
        var verPath = versionsPath.to(version.getJsonName());
        var verJsonPath = verPath.to(version.getJsonName() + ".json");
        if (verJsonPath.exists() && !disableCache)
            return;


        var art = version.fArtifacts.stream().filter(x -> x.type == FArtifact.Type.INSTALLER).findFirst().orElse(null);
        if (art == null)
            return;

        Vanilla.getVanilla().install(version);

        try{
            logState(".forge.state.download");
            var path = Configurator.getConfig().getTemporaryFolder();
            path = NetUtils.download(art.getUrl(), path, true, null);

            var target = Configurator.getConfig().getGamePath().toFile();

            logState(".forge.state.install");

            var profileInfo = generateProfileInfo(Path.begin(target.toPath()));
            try(URLClassLoader loader = new URLClassLoader(new URL[]{path.toFile().toURI().toURL()})){
                try{
                    var loadInstallProfile = loader.loadClass("net.minecraftforge.installer.json.Util").getMethod("loadInstallProfile");

                    var v1 = loadInstallProfile.invoke(null);

                    var cls = loader.loadClass("net.minecraftforge.installer.actions.ClientInstall");
                    var callbackInterface = loader.loadClass("net.minecraftforge.installer.actions.ProgressCallback");
                    var monitor = Proxy.newProxyInstance(callbackInterface.getClassLoader(), new Class[]{callbackInterface}, (proxy, method, args) -> {
                        if (method.getName().equals("message") && args.length > 0)
                            logState("," + args[0].toString() + ":." + "forge.state.install");
                        return null;
                    });
                    //var monitor = loader.loadClass("net.minecraftforge.installer.actions.ProgressCallback").getDeclaredField("TO_STD_OUT").get(null);
                    var clientInstall = cls.getConstructors()[0].newInstance(v1,monitor );

                    var method = cls.getMethod("run", File.class, Predicate.class, File.class);
                    method.invoke(clientInstall, target, (Predicate<String>) a -> true, path.toFile());
                }
                catch (NoSuchMethodException | ClassNotFoundException ignored){
                    var cls = loader.loadClass("net.minecraftforge.installer.ClientInstall");
                    var clientInstall = cls.getConstructors()[0].newInstance();

                    var srv = loader.loadClass("net.minecraftforge.installer.ServerInstall");
                    var serverInstall = srv.getConstructors()[0].newInstance();

                    var actType = loader.loadClass("net.minecraftforge.installer.ActionType");

                    var predicates = loader.loadClass("com.google.common.base.Predicates");
                    var predicate = loader.loadClass("com.google.common.base.Predicate");

                    var always = predicates.getMethod("alwaysTrue").invoke(null);
                    var method = actType.getMethod("run", File.class, predicate);

                    method.invoke(clientInstall, target, always);
                    method.invoke(serverInstall, target, always);
                }
            }
            catch (Exception e){
                Logger.getLogger().logHyph("ERRFORGE " + version.getWrapperVersion());
                Logger.getLogger().log(e);
                logState(".error.unknown");
            }

            logState(".launch.state.finish");

            path.delete();
            getGameDir().getFiles().forEach(x -> {
                if (x.getExtension() != null && x.getExtension().equals("jar"))
                    x.delete();
            });

            var read = new Gson().fromJson(profileInfo.read(), JsonObject.class);
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
        catch (NoConnectionException e){
            throw e;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        //profileInfo.delete();
    }
}
