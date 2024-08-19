package com.laeben.corelauncher.minecraft.entity;

import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.entity.OS;

import java.util.List;
import java.util.regex.Pattern;

public class Library {

    public DownloadOptions downloads;
    public String name;
    public String url;
    public transient String fileName;
    public List<Rule> rules;

    public Extract extract;

    public boolean checkAvailability(OS os){
        return Rule.checkRules(rules, os);
    }

    public Asset getAsset(){
        var main = getMainAsset();
        if (main != null){
            main.base = name;
            return main;
        }
        return getNativeAsset();
    }
    public Asset getNativeAsset(){
        return downloads != null ? (downloads.classifiers != null ? downloads.classifiers.getNatives(CoreLauncher.SYSTEM_OS) : null) : null;
    }
    public Asset getMainAsset(){
        if (downloads == null){
            String path = calculatePath();
            return new Asset(path, url + path);
        }
        return downloads.artifact != null ? downloads.artifact : null;
    }

    public String calculatePath(){
        String[] spl = name.replace("[", "").replace("]", "").split(":");
        String path = spl[0].replace('.', '/');
        String ext = "jar";
        String last = spl[spl.length - 1];
        if (last.contains("@")){
            var lst = last.split("@");
            ext = lst[1];
            last = lst[0];
        }

        fileName = "";

        for (int i = 1; i < spl.length - 1; i++){
            path += "/" + spl[i];
            fileName += spl[i] + "-";
        }
        fileName += last + "." + ext;
        path += "/" + last + "/" + fileName;


        return path;
    }
}
