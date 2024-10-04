package com.laeben.corelauncher;

import com.laeben.corelauncher.wrap.ExtensionWrapper;

public class Debug {

    public static final boolean DEBUG = false;
    public static final boolean DEBUG_UI = false;

    public static void run(){
        //var profile = Profiler.getProfiler().getProfile("Valhelsia 60");
        var wrapper = new ExtensionWrapper();
        wrapper.reload();


        int x = 0;
    }

    public static void runUI(){


    }
}
