package com.laeben.corelauncher.util.java.entity;

import com.laeben.corelauncher.api.entity.OS;

public record JavaDownloadInfo(String name, String url, String displayName, int major, OS os){

}