package com.cdev.corelauncher.minecraft.entities;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.utils.OSUtils;
import com.cdev.corelauncher.utils.entities.OS;
import com.google.gson.JsonObject;

import java.util.List;

public class Rule {
    public static class RuleOS {
        public String arch;
        public OS name;

        public boolean is64BitOS(){
            return arch != null && arch.equals("x64");
        }
    }
    public String action;
    public RuleOS os;
    public JsonObject features;

    public boolean allow(){
        return action.equals("allow");
    }
    public boolean checkOS(OS os){
        return this.os.name == os || (this.os.is64BitOS() && CoreLauncher.OS_64);
    }
    public boolean checkFeature(String feature){
        if (features == null)
            return false;

        return features.has(feature);
    }

    public static boolean checkRules(List<Rule> rules, OS os){
        return rules == null || rules.stream().anyMatch(y -> y.action.equals("allow") && (y.os == null || y.checkOS(os)));
    }
}
