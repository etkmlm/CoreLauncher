package com.cdev.corelauncher.minecraft.entities;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.utils.CommandConcat;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;

import java.util.List;
import java.util.Objects;

public class ExecutionInfo{
    public String versionId;
    public Account account;
    public Java java;
    public Path dir;
    public String[] args;

    public ExecutionInfo(String versionId, Account account, Java java, Path dir, String... args){
        this.versionId = versionId;
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

        return new ExecutionInfo(profile.getVersionId(), profile.getUser(), profile.getJava(), profile.getPath(), concat.generate().toArray(new String[0]));
    }

    public class LaunchInfo {

        public LaunchInfo() {
            var gameDir = Configurator.getConfig().getGamePath();
            versionDir = gameDir.to("versions", versionId);
            nativePath = versionDir.to("natives");
            jsonPath = versionDir.to(versionId + ".json");
            clientPath = versionDir.to(versionId + ".jar");

            var v = new Gson().fromJson(jsonPath.read(), Version.class);
            assets = v.assetIndex;
            java = v.javaVersion == null ? Java.fromCodeName("legacy") : v.javaVersion;
            mainClass = v.mainClass;
            arguments = v.minecraftArguments;

            libraries = v.libraries.stream()
                    .filter(x -> x.rules == null || x.rules.stream().anyMatch(y -> y.action.equals("allow") && (y.os == null || y.checkOS(CoreLauncher.SYSTEM_OS))))
                    .map(x -> x.downloads.artifact != null ? x.downloads.artifact : x.downloads.classifiers.getNatives(CoreLauncher.SYSTEM_OS))
                    .filter(Objects::nonNull)
                    .map(x -> gameDir.to("libraries", x.path).toString()).toList();
        }

        public List<String> libraries;
        public Path clientPath;
        public Path jsonPath;
        public Path versionDir;
        public Path nativePath;
        public Asset assets;

        public Java java;
        public String mainClass;
        public String arguments;
    }

}