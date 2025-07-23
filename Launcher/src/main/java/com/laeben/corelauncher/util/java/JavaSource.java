package com.laeben.corelauncher.util.java;

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
        put(JavaSourceType.AZUL, new AzulJavaManager());
        put(JavaSourceType.ADOPTIUM, new AdoptiumJavaManager());
    }};

    JavaDownloadInfo getJavaInfo(Java j, OS os, boolean is64Bit) throws NoConnectionException, HttpException;
}
