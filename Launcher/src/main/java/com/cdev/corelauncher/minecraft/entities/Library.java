package com.cdev.corelauncher.minecraft.entities;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.utils.entities.OS;

import java.util.List;

public class Library {
    public DownloadOptions downloads;
    public String name;
    public transient String fileName;
    public List<Rule> rules;

    public Extract extract;

    public boolean checkAvailability(OS os){
        return Rule.checkRules(rules, os);
    }

    public Asset getMainAsset(){
        return downloads == null ? new Asset(calculatePath(), null, -1) :
                (downloads.artifact != null ? downloads.artifact : downloads.classifiers.getNatives(CoreLauncher.SYSTEM_OS));
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
