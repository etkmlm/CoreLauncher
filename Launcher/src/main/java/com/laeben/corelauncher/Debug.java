package com.laeben.corelauncher;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.util.java.JavaManager;
import com.laeben.corelauncher.util.java.entity.JavaSourceType;

public class Debug {

    public static final boolean DEBUG = false;
    public static final boolean DEBUG_UI = false;

    public static void run(){
        //var profile = Profiler.getProfiler().getProfile("Valhelsia 60");
        var shortcutPath = Path.begin(java.nio.file.Path.of("C:/Users/furka/Desktop/java"));
        //var k = GsonUtil.DEFAULT_GSON.fromJson(js, Instructor.class);

        try {
            JavaManager.getManager().setSourceType(JavaSourceType.ADOPTIUM);
            var info = JavaManager.getManager().getJavaInfo(Java.fromVersion(17), "aarch64", OS.OSX);
            var j = JavaManager.getManager().downloadAndExtract(info, shortcutPath, null);

            int m = 0;
        } catch (NoConnectionException | StopException e) {
            throw new RuntimeException(e);
        }

        int x = 0;
    }

    public static void runUI(){

    }
}
