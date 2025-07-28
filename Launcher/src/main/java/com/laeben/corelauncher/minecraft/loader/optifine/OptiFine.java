package com.laeben.corelauncher.minecraft.loader.optifine;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.loader.Vanilla;
import com.laeben.corelauncher.minecraft.loader.optifine.entity.OptiVersion;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.core.entity.Path;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OptiFine extends Loader<OptiVersion> {

    private static final String BASE_URL = "https://optifine.net/";
    private static final String OPTI_INDEX = BASE_URL + "downloads";
    private static final List<OptiVersion> cache = new ArrayList<>();
    private static final Pattern VP = Pattern.compile(".*OptiFine_(.+)_HD.*");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM.dd.yyyy");
    private static OptiFine instance;

    public void asInstance(){
        instance = this;
    }

    public static OptiFine getOptiFine(){
        return instance;
    }

    @Override
    public OptiVersion getVersion(String id, String wrId) {
        logState("acqVersionOptiFine - " + id);
        var version = getAllVersions().stream().filter(x -> x.checkId(id) && x.getLoaderVersion().equals(wrId)).findFirst().orElse(null);
        if (version == null)
            return null;

        refreshUrl(version);

        return version;
    }

    public void refreshUrl(OptiVersion version){
        if (version.url != null || version.rawUrl == null)
            return;

        try{
            var doc = NetUtil.getDocumentFromUrl(version.rawUrl);
            version.url = BASE_URL + doc.getElementsByClass("downloadButton").get(0).getElementsByTag("a").get(0).attr("href");
        }
        catch (NoConnectionException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    @Override
    public List<OptiVersion> getAllVersions() {

        if (!cache.isEmpty() && !disableCache)
            return cache;
        cache.clear();

        try{
            var doc = NetUtil.getDocumentFromUrl(OPTI_INDEX);
            var all = doc.selectXpath("//tr[@class='downloadLine downloadLineMain']|//tr[@class='downloadLine downloadLineMore']|//tr[@class='downloadLine downloadLinePreview']");

            for (var a : all){
                String text = a.getElementsByClass("colFile").get(0).text();
                String rawUrl = a.getElementsByClass("colMirror").get(0).getElementsByTag("a").get(0).attr("href");
                String forge = a.getElementsByClass("colForge").get(0).text().split(" ")[1];
                String date = a.getElementsByClass("colDate").get(0).text();

                var m = VP.matcher(rawUrl);
                String version = m.matches() ? m.group(1) : null;

                if (version == null)
                    continue;

                OptiVersion v = new OptiVersion().setLoaderVersion(text);
                v.forgeLoaderVersion = forge;
                v.rawUrl = rawUrl;
                v.id = version;
                v.releaseTime = DATE_FORMAT.parse(date);

                cache.add(v);
            }
        }
        catch (NoConnectionException e){
            return getOfflineVersions();
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }


        return cache;
    }

    @Override
    public List<OptiVersion> getVersions(String id) {
        logState("acqVersionOptiFine - " + id);
        return getAllVersions().stream().filter(x -> x.checkId(id)).toList();
    }

    @Override
    public OptiVersion getVersionFromIdentifier(String identifier, String inherits){
        if (inherits == null)
            inherits = "*";
        return identifier.toLowerCase().contains("optifine") ? new OptiVersion(inherits, identifier.split("-")[1].replace('_', ' ')) : null;
    }

    public static void installForge(OptiVersion get, Path modsFolder) throws NoConnectionException, StopException, HttpException, FileNotFoundException {
        instance.refreshUrl(get);

        var path = Configurator.getConfig().getTemporaryFolder();
        String fileName = get.getJsonName() + ".jar";
        path = NetUtil.download(get.url, path.to(fileName), false, false);

        path.move(modsFolder.to(fileName));
    }

    @Override
    public void install(OptiVersion v) throws StopException, NoConnectionException {
        Vanilla.getVanilla().install(v);

        var gameDir = Configurator.getConfig().getGamePath();
        String name = v.getJsonName();
        var jsonPath = gameDir.to("versions", name, name + ".json");
        var clientPath = gameDir.to("versions", name, name + ".jar");
        if (clientPath.exists() && !disableCache)
            return;

        try{
            logState(".optifine.state.download");

            var profileInfo = generateProfileInfo(gameDir);
            var path = Configurator.getConfig().getTemporaryFolder();

            refreshUrl(v); // We need to refresh it

            path = NetUtil.download(v.url, path.to(clientPath.getName()), false, false);

            if (stopRequested)
                throw new StopException();

            logState(".optifine.state.install");
            try(URLClassLoader loader = new URLClassLoader(new URL[]{path.toFile().toURI().toURL()})){
                var installer = loader.loadClass("optifine.Installer");
                var doInstall = installer.getMethod("doInstall", File.class);

                doInstall.invoke(null, gameDir.toFile());
            }
            catch (Exception e){
                Logger.getLogger().log(e);
                logState(UNKNOWN_ERROR);
            }

            path.delete();


            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            var f = gson.fromJson(profileInfo.read(), JsonObject.class);
            if (f != null){
                var obj = f.get("profiles").getAsJsonObject().get("OptiFine").getAsJsonObject();
                if (!obj.has("javaArgs"))
                    return;
                String args = obj.get("javaArgs").getAsString();
                if (args != null){
                    String[] jvm = args.split(" ");
                    var g = gson.fromJson(jsonPath.read(), JsonObject.class);
                    var array = new JsonArray();
                    for(String j : jvm)
                        array.add(j);
                    g.get("arguments").getAsJsonObject().add("jvm", array);

                    String str = gson.toJson(g);
                    jsonPath.write(str);
                }
            }
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        logState(LAUNCH_FINISH);
    }


    @Override
    public LoaderType getType() {
        return LoaderType.OPTIFINE;
    }
}
