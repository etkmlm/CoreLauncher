package com.laeben.corelauncher.util.java;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.util.java.entity.JavaDownloadInfo;
import com.laeben.corelauncher.util.java.entity.JavaSourceType;

import java.util.HashMap;
import java.util.Map;

public interface JavaSource {
    Map<JavaSourceType, JavaSource> SOURCES = new HashMap<>(){{
        put(JavaSourceType.AZUL, new AzulJavaSource());
        put(JavaSourceType.ADOPTIUM, new AdoptiumJavaSource());
    }};

    JavaDownloadInfo getJavaInfo(Java j, OS os, String arch) throws NoConnectionException, HttpException;
    void extract(Path archive, JavaDownloadInfo info);
}
