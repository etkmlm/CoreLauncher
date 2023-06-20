package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.utils.OSUtils;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Config {
    private static final Path DEFAULT_GAME_PATH = new Path(OSUtils.getAppFolder());

    private Account user;
    private Path gamePath;
    private Locale language;
    private Path backgroundImage;
    private Profile lastSelectedProfile;
    private List<Java> customJavaVersions;
    private Java defaultJava;
    private int defaultMinRAM;
    private int defaultMaxRAM;
    private boolean showOldReleases;
    private boolean showSnapshots;
    private boolean logMode;
    private boolean hideAfter;

    public Path getGamePath(){
        return gamePath == null ? DEFAULT_GAME_PATH : gamePath;
    }
    public Path getLauncherPath(){
        return getGamePath().to("launcher");
    }
    public Path getTemporaryFolder(){
        return getLauncherPath().to("temp");
    }

    public void setGamePath(Path gamePath){
        this.gamePath = gamePath;
    }

    public void setLanguage(Locale l){
        this.language = l;
    }
    public void setDefaultJava(Java j){
        defaultJava = j;
    }

    public Java getDefaultJava(){
        return defaultJava;
    }
    public void setDefaultMinRAM(int minRam){
        defaultMinRAM = minRam;
    }

    public int getDefaultMinRAM(){
        return defaultMinRAM;
    }
    public void setDefaultMaxRAM(int maxRAM){
        defaultMaxRAM = maxRAM;
    }

    public Path getBackgroundImage(){
        return backgroundImage != null ? (backgroundImage.exists() ? backgroundImage : null) : null;
    }

    public void setBackgroundImage(Path img){
        this.backgroundImage = img;
    }

    public int getDefaultMaxRAM(){
        return defaultMaxRAM;
    }

    public boolean isShowOldReleases() {
        return showOldReleases;
    }

    public void setShowOldReleases(boolean showOldReleases) {
        this.showOldReleases = showOldReleases;
    }

    public boolean isShowSnapshots() {
        return showSnapshots;
    }

    public void setShowSnapshots(boolean showSnapshots) {
        this.showSnapshots = showSnapshots;
    }

    public void setLastSelectedProfile(Profile p){
        lastSelectedProfile = p;
    }
    public void setLogMode(boolean mode){
        logMode = mode;
    }
    public boolean getLogMode(){
        return logMode;
    }
    public boolean hideAfter(){
        return hideAfter;
    }
    public void setHideAfter(boolean ha){
        hideAfter = ha;
    }
    public Profile getLastSelectedProfile(){
        return lastSelectedProfile;
    }
    public List<Java> getCustomJavaVersions(){
        if (customJavaVersions == null)
            customJavaVersions = new ArrayList<>();
        return customJavaVersions;
    }

    public Account getUser(){
        return user;
    }

    public Locale getLanguage(){
        return language;
    }

}
