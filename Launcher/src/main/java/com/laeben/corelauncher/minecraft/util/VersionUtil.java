package com.laeben.corelauncher.minecraft.util;

import java.util.Iterator;

public class VersionUtil {
    public static int calculateVersionValue(String version){
        try{
            /*StringBuilder builder = new StringBuilder();
            for (int i = 0; i < version.length(); i++) {
                char c = version.charAt(i);

                if (c != '.' && c != 'w' && c != 'a')
                    builder.append(c);

            }*/

            return Integer.parseInt(
                    version.replace(".", "")
            );
        }
        catch (NumberFormatException ignored){}

        return 0;
        //return -1;
    }
    public static String getNewestVersion(Iterator<String> versions){
        int value = -1;
        String version = null;
        while (versions.hasNext()){
            var v = versions.next();
            var n = calculateVersionValue(v);
            if (n > value){
                value = n;
                version = v;
            }
        }
        return version;
    }
}
