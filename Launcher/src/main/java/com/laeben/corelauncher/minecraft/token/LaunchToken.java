package com.laeben.corelauncher.minecraft.token;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.entity.Path;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.core.network.entity.NetworkToken;
import com.laeben.corelauncher.api.entity.FileCheckMode;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.loader.entity.RedownloadSettings;

public class LaunchToken extends CancellableToken<LaunchToken> {
    private final Profile profile;
    private final FileCheckMode checkMode;
    private final RedownloadSettings redownloadSettings;
    private final ProgressFunction onProgress;

    private LaunchToken(Profile profile, FileCheckMode checkMode, RedownloadSettings redownloadSettings, ProgressFunction onProgress){
        this.profile = profile;
        this.checkMode = checkMode;
        this.redownloadSettings = redownloadSettings == null ? RedownloadSettings.none() : redownloadSettings;
        this.onProgress = onProgress;
    }

    public static LaunchToken create(Profile profile, FileCheckMode checkMode, RedownloadSettings redownloadSettings, ProgressFunction onProgress){
        return new LaunchToken(profile, checkMode, redownloadSettings, onProgress);
    }

    public NetworkToken toNetworkToken(String url, Path path, boolean useOriginalName){
        return NetworkToken.create(url, path, useOriginalName).withLogging(onProgress).syncWith(this);
    }

    public Profile getProfile() {
        return profile;
    }
    public ProgressFunction getOnProgress(){
        return onProgress;
    }
    public FileCheckMode getFileCheckMode() {
        return checkMode;
    }

    public RedownloadSettings getRedownloadSettings(){
        return redownloadSettings;
    }

    @Override
    public String toString() {
        return getProfile().getName();
    }
}
