package com.laeben.corelauncher.minecraft.entity;

import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.util.ArgumentConcat;
import com.laeben.corelauncher.minecraft.util.CommandConcat;
import com.laeben.corelauncher.minecraft.util.LibraryConcat;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.core.entity.Path;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionInfo{
    public String executor;
    public Version version;
    public Account account;
    public Java java;
    public Path dir;
    public String[] args;
    public boolean ignoreAuth;

    public ServerInfo server;

    public ExecutionInfo(String executor, Version v, Account account, Java java, Path dir, String... args){
        this.executor = executor;
        this.version = v;
        this.account = account;
        this.java = java;
        this.dir = dir;
        this.args = args;
    }

    public static ExecutionInfo fromProfile(Profile profile){
        var concat = new CommandConcat().add(profile.getJvmArgs());

        int pMax = profile.getMaxRAM();
        int pMin = profile.getMinRAM();
        int cMax = Configurator.getConfig().getDefaultMaxRAM();
        int cMin = Configurator.getConfig().getDefaultMinRAM();

        if (pMax < pMin)
            profile.setMaxRAM(pMin);

        if (pMax != 0)
            concat.add("-Xmx" + pMax + "M");

        if (pMin != 0)
            concat.add("-Xms" + pMin + "M");

        if (pMin == 0 && pMax == 0){
            if (cMin != 0)
                concat.add("-Xms" + cMin + "M");
            if (cMax != 0)
                concat.add("-Xmx" + cMax + "M");
        }

        return new ExecutionInfo(profile.getName(), profile.getWrapper().getVersion(profile.getVersionId(), profile.getWrapperVersion()), profile.getUser(), profile.getJava(), profile.getPath(), concat.generate().toArray(new String[0]));
    }

    public ExecutionInfo includeServer(ServerInfo info){
        this.server = info;

        return this;
    }

    public ExecutionInfo ignoreAuthentication(){
        ignoreAuth = true;

        return this;
    }

    public class LaunchInfo {

        public List<String> libraries;
        public Path clientPath;
        public Path wrappedJsonPath;
        public Path wrappedClientPath;
        public Path jsonPath;
        public Path versionDir;
        public Path nativePath;
        public Asset assets;

        public Java java;
        public String mainClass;
        public List<String> agents;
        private final ArgumentConcat jvmArguments;
        private final ArgumentConcat gameArguments;

        public LaunchInfo() throws VersionNotFoundException {
            var gameDir = Configurator.getConfig().getGamePath();
            versionDir = gameDir.to("versions", version.id);
            nativePath = versionDir.to("natives");
            jsonPath = versionDir.to(version.id + ".json");
            clientPath = versionDir.to(version.id + ".jar");

            var wrappedPath = gameDir.to("versions", version.getJsonName());
            wrappedJsonPath = wrappedPath.to(version.getJsonName() + ".json");
            wrappedClientPath = wrappedPath.to(version.getClientName() + ".jar");


            Logger.getLogger().logHyphBlock("EXECUTION", String.format("Game Dir: %s\nNative Path: %s\nJSON Path: %s\nClient Path: %s\nWrapped Json Path: %s\nWrapped Client Path: %s", gameDir, nativePath, jsonPath, clientPath, wrappedJsonPath, wrappedClientPath));

            //versionDir = gameDir.to("versions", version.id);
            //            nativePath = versionDir.to("natives");
            //            jsonPath = versionDir.to(version.id + ".json");
            //            wrappedJsonPath = gameDir.to("versions", version.getJsonName(), version.getJsonName() + ".json");
            //            clientPath = versionDir.to(version.getClientName() + ".jar");

            var v0 = GsonUtil.EMPTY_GSON.fromJson(jsonPath.read(), Version.class);
            if (v0 == null)
                throw new VersionNotFoundException("Cannot find path " + jsonPath);
            var v = GsonUtil.EMPTY_GSON.fromJson(wrappedJsonPath.read(), Version.class);
            if (v == null)
                throw new VersionNotFoundException("Cannot find path " + wrappedJsonPath);
            assets = v0.getAssetIndex();
            java = v0.javaVersion == null ? Java.fromCodeName("legacy") : v0.javaVersion;
            mainClass = v.mainClass;

            gameArguments = extractArguments(v, true).concat(extractArguments(v0, true));
            jvmArguments = extractArguments(v, false).concat(extractArguments(v0, false));

            var libDir = gameDir.to("libraries");

            libraries = LibraryConcat.begin(libDir)
                    .addLibraries(v0.libraries)
                    .addLibraries(v.libraries)
                    .build()
                    .paths();

            /*libraries = Stream.concat(v.libraries.stream(), v0.libraries.stream())
                    .filter(x -> x.checkAvailability(CoreLauncher.SYSTEM_OS))
                    .map(Library::getAsset)
                    .filter(Objects::nonNull)
                    .map(x -> libDir.to(x.path).toString())
                    .distinct()
                    .toList();*/

            agents = Arrays.stream(LauncherConfig.LAUNCHER_LIBRARIES).filter(x -> x.isAgent).map(x -> "-javaagent:" + libDir.to(x.calculatePath())).toList();
            //agentPath = libDir.to(Arrays.stream(LauncherConfig.LAUNCHER_AGENTS).filter(x -> x.fileName.startsWith("clfixer")).findFirst().orElse(new Library()).calculatePath());
        }

        private ArgumentConcat extractArguments(Version v, boolean isGame){
            if (v.minecraftArguments != null && isGame)
                return new ArgumentConcat(Arrays.stream(v.minecraftArguments.split(" ")).collect(Collectors.toList()));
            if (v.arguments == null)
                return new ArgumentConcat(new ArrayList<>());

            if (isGame){
                if (v.arguments.game == null)
                    return new ArgumentConcat(new ArrayList<>());
                //x.isJsonPrimitive() ? x.getAsString() : (x.getAsJsonObject().get("rules").getAsJsonArray())
                return new ArgumentConcat(v.arguments.game.stream().filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsString).collect(Collectors.toList()));
            }
            else{
                var gson = new Gson();
                if (v.arguments.jvm == null)
                    return new ArgumentConcat(new ArrayList<>());
                return new ArgumentConcat(v.arguments.jvm.stream().filter(x -> {
                    if (x.isJsonPrimitive())
                        return true;

                    var a = x.getAsJsonObject();
                    var rules = a.get("rules").getAsJsonArray().asList().stream().map(y -> gson.fromJson(y, Rule.class)).toList();
                    return Rule.checkRules(rules, CoreLauncher.SYSTEM_OS);
                }).map(x -> {
                    if (x.isJsonPrimitive())
                        return x.getAsString();
                    var value = x.getAsJsonObject().get("value");
                    if (value.isJsonPrimitive())
                        return value.getAsString();
                    return value.getAsJsonArray().get(0).getAsString(); // TODO Look at future
                }).collect(Collectors.toList()));
            }
        }

        public String[] getGameArguments(){
            var gameDir = Configurator.getConfig().getGamePath();

            Path assetsRoot;
            if (assets.isVeryLegacy()){
                assetsRoot = gameDir.to("assets", "virtual", "verylegacy");
            }
            else if (assets.isLegacy()){
                assetsRoot = gameDir.to("assets", "virtual", "legacy");
            }
            else{
                assetsRoot = gameDir.to("assets");
            }

            return gameArguments
                    .register("${auth_player_name}", account.getUsername())
                    .register("${version_name}", version.id)
                    .register("${game_directory}", dir.toString())
                    .register("${assets_root}", assetsRoot.toString())
                    .register("${game_assets}", assetsRoot.toString())
                    .register("${assets_index_name}", assets.id)
                    .register("${auth_uuid}", account.reload().getUuid())
                    .register("${auth_access_token}", account.getCachedToken())
                    .register("${user_properties}", "{}")
                    .register("${user_type}", "msa")
                    .register("${version_type}", version.type == null ? "release" : version.type)
                    .register("${clientid}", version.id)
                    //.register("${auth_xuid}", "null")
                    .disable("--xuid")
                    .register("${auth_session}", "X")
                    .include("--quickPlayMultiplayer", server == null ? null : server.toString())
                    .include("--server", server == null ? null : server.toString())
                    .build();
        }

        public String[] getJvmArguments(){
            var gameDir = Configurator.getConfig().getGamePath();
            return jvmArguments
                    .register("${natives_directory}", nativePath.toString())
                    .register("${classpath_separator}", String.valueOf(File.pathSeparatorChar))
                    .register("${launcher_name}", LauncherConfig.APPLICATION.getName())
                    .register("${launcher_version}", String.valueOf(LauncherConfig.VERSION))
                    .register("${library_directory}", gameDir.to("libraries").toString())
                    .register("${version_name}", version.id)
                    .disable("-Djava.library.path")
                    .disable("-Dminecraft.launcher")
                    .disable("-cp")
                    .build();
        }
    }

}