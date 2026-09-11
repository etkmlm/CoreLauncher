package com.laeben.corelauncher.minecraft.token;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.corelauncher.api.entity.FileCheckMode;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.loader.entity.RedownloadSettings;

public class VersionToken<T extends Version> extends CancellableToken<VersionToken<T>> {
    private final T version;
    private final FileCheckMode fileCheckMode;
    private final RedownloadSettings redownloadSettings;
    private ProgressFunction onProgress;

    private VersionToken(T version, FileCheckMode fileCheckMode, RedownloadSettings redownloadSettings, ProgressFunction onProgress) {
        this.version = version;
        this.fileCheckMode = fileCheckMode;
        this.redownloadSettings = redownloadSettings == null ? RedownloadSettings.none() : redownloadSettings;
        this.onProgress = onProgress;
    }

    public static <T extends Version> VersionToken<T> create(T version, FileCheckMode fileCheckMode, RedownloadSettings redownloadSettings, ProgressFunction onProgress) {
        return new VersionToken(version, fileCheckMode, redownloadSettings, onProgress);
    }

    public VersionToken<T> withProgressLogging(ProgressFunction onProgress){
        this.onProgress = onProgress;
        return this;
    }

    public T getVersion(){
        return version;
    }
    public ProgressFunction getOnProgress(){
        return onProgress;
    }
    public FileCheckMode getFileCheckMode(){
        return fileCheckMode;
    }

    public RedownloadSettings getRedownloadSettings(){
        return redownloadSettings;
    }
}
