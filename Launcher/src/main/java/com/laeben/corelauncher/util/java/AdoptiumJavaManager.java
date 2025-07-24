package com.laeben.corelauncher.util.java;

import com.google.gson.JsonArray;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.util.java.entity.JavaDownloadInfo;

public class AdoptiumJavaManager implements JavaSource {
    private static final String ADOPTIUM = "https://api.adoptium.net/v3/assets/latest/";

    @Override
    public JavaDownloadInfo getJavaInfo(Java j, OS os, String arch) throws NoConnectionException, HttpException {
        if (arch != null){
            if (arch.equals("amd64"))
                arch = "x64";
            else if (arch.equals("i686"))
                arch = "x86";
        }

        String url = String.format("%s%d/hotspot?os=%s&image_type=jre&architecture=%s", ADOPTIUM, j.majorVersion, os.getName(), arch);

        var arr = GsonUtil.EMPTY_GSON.fromJson(NetUtil.urlToString(url), JsonArray.class);
        if (arr == null || arr.isEmpty())
            return null;
        var object = arr.get(0);
        if (object == null)
            return null;
        var obj = object.getAsJsonObject();
        return new JavaDownloadInfo(
                obj.get("release_name").getAsString() + "-jre",
                obj.getAsJsonObject("binary")
                        .getAsJsonObject("package")
                        .get("link")
                        .getAsString(),
                "Adoptium JRE " + j.majorVersion,
                j.majorVersion);
    }
}
