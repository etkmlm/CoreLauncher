package com.laeben.corelauncher.util.java;

import com.google.gson.JsonArray;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.util.GsonUtil;

public class AzulJavaManager extends JavaManager {
    private static final String ZULU = "https://api.azul.com/metadata/v1/zulu/packages/";

    private static String getZuluOS(OS os) {
        return switch (os) {
            case OSX -> "macos";
            case WINDOWS -> "windows";
            default -> "linux";
        };
    }

    @Override
    public JavaDownloadInfo getJavaInfo(Java j, OS os, boolean is64Bit) throws NoConnectionException, HttpException {
        String url = String.format("%s?java_version=%s&os=%s&arch=%s&java_package_type=jdk&javafx_bundled=true&availability_types=CA&release_status=ga&certifications=tck", ZULU, j.majorVersion, getZuluOS(os), is64Bit ? "x64" : "x86");

        var arr = GsonUtil.EMPTY_GSON.fromJson(NetUtil.urlToString(url), JsonArray.class);
        if (arr == null || arr.isEmpty())
            return null;
        var object = arr.get(0);
        if (object == null)
            return null;
        var obj = object.getAsJsonObject();
        return new JavaDownloadInfo(
                obj.get("name").getAsString()
                        .replace(".tar.gz", "")
                        .replace(".zip", ""),
                obj.get("download_url").getAsString(),
                "Zulu JDK " + j.majorVersion,
                j.majorVersion
        );
    }
}
