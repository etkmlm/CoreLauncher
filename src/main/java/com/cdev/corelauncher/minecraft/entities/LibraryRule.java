package com.cdev.corelauncher.minecraft.entities;

import com.cdev.corelauncher.utils.entities.OS;

public class LibraryRule {
    public static class LibraryRuleOS{
        public OS name;
    }
    public String action;
    public LibraryRuleOS os;

    public boolean checkOS(OS os){
        return this.os.name == os;
    }
}
