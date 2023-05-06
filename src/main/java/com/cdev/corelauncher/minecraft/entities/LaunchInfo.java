package com.cdev.corelauncher.minecraft.entities;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LaunchInfo {
    public LaunchInfo(Launcher l, String id) throws IOException {
        versionId = id;
        versionDir = l.getGameDir().to("versions", id);
        nativePath = versionDir.to("natives");
        jsonPath = versionDir.to(id + ".json");
        clientPath = versionDir.to(id + ".jar");

        var v = new Gson().fromJson(jsonPath.read(), Version.class);
        assetVersion = v.assets;
        java = v.javaVersion;
        mainClass = v.mainClass;

        libraries = v.libraries.stream()
                .filter(x -> x.rules == null || x.rules.stream().anyMatch(y -> y.action.equals("allow") && (y.os == null || y.checkOS(CoreLauncher.SYSTEM_OS))))
                .map(x -> x.downloads.artifact != null ? x.downloads.artifact : x.downloads.classifiers.getNatives(CoreLauncher.SYSTEM_OS))
                .map(x -> l.getGameDir().to("libraries", x.path).toString()).toList();
    }

    public String versionId;

    public List<String> libraries;
    public Path clientPath;
    public Path jsonPath;
    public Path versionDir;
    public Path nativePath;
    public String assetVersion;

    public Java java;
    public String mainClass;
}
