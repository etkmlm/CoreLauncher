package com.laeben.corelauncher.util.java;

import com.google.gson.JsonArray;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.util.java.entity.JavaDownloadInfo;

public class AzulJavaSource implements JavaSource {
    private static final String ZULU = "https://api.azul.com/metadata/v1/zulu/packages/";

    private static String getZuluOS(OS os) {
        return switch (os) {
            case OSX -> "macos";
            case WINDOWS -> "windows";
            default -> "linux";
        };
    }

    @Override
    public JavaDownloadInfo getJavaInfo(Java j, OS os, String arch) throws NoConnectionException, HttpException {
        String url = String.format("%s?java_version=%s&os=%s&arch=%s&java_package_type=%s&javafx_bundled=false&availability_types=CA&release_status=ga&certifications=tck&page_size=1&archive_type=%s", ZULU, j.majorVersion, getZuluOS(os), arch, JavaManager.PACKAGE_TYPE, os == OS.WINDOWS ? "zip" : "tar_gz");

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
                String.format("Zulu %s %d", JavaManager.PACKAGE_TYPE.toUpperCase(), j.majorVersion),
                j.majorVersion,
                os
        );
    }

    @Override
    public void extract(Path archive, JavaDownloadInfo info) {
        archive.extract(null, null);
        if (info.os() != OS.OSX)
            return;

        var folder = archive.parent().to(info.name());
        var home = folder.to(String.format("zulu-%d.%s", info.major(), JavaManager.PACKAGE_TYPE), "Contents", "Home");
        var temp = folder.parent().to("temp");
        home.move(temp);
        folder.delete();
        temp.move(folder);
    }
}
