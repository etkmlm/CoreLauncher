package com.laeben.corelauncher.util.java;

import com.google.gson.JsonArray;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.util.GsonUtil;

public class AdoptiumJavaManager extends JavaManager{
    private static final String ADOPTIUM = "https://api.adoptium.net/v3/assets/latest/";

    @Override
    public JavaDownloadInfo getJavaInfo(Java j, OS os, boolean is64Bit) throws NoConnectionException, HttpException {
        String url = ADOPTIUM + j.majorVersion + "/hotspot?os=" + os.getName() + "&image_type=jdk&architecture=" + (is64Bit ? "x64" : "x86");

        var arr = GsonUtil.EMPTY_GSON.fromJson(NetUtil.urlToString(url), JsonArray.class);
        if (arr == null || arr.isEmpty())
            return null;
        var object = arr.get(0);
        if (object == null)
            return null;
        var obj = object.getAsJsonObject();
        return new JavaDownloadInfo(
                obj.get("release_name").getAsString(),
                obj.getAsJsonObject("binary")
                        .getAsJsonObject("package")
                        .get("link")
                        .getAsString(),
                "Adoptium JDK " + j.majorVersion,
                j.majorVersion);
    }
}
