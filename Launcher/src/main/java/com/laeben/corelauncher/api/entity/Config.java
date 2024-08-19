package com.laeben.corelauncher.api.entity;

import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.core.entity.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Config {
    private static final Path DEFAULT_GAME_PATH = Path.begin(OSUtil.getAppFolder());

    private Account user;
    private Path gamePath;
    private Path lastBackupPath;
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
    private boolean debugLogMode;
    private boolean hideAfter;
    private boolean autoUpdate;
    private boolean delGameLogs;
    private boolean showHelloDialog;
    private boolean placeNewProfileToDock;
    private List<Integer> announces;

    public Path getGamePath(){
        return (gamePath == null ? DEFAULT_GAME_PATH : gamePath).forceSetDir(true);
    }
    public Path getLauncherPath(){
        return getGamePath().to("launcher");
    }
    public Path getTemporaryFolder(){
        return getLauncherPath().to("temp");
    }
    public Path getImagePath(){
        return getLauncherPath().to("images");
    }

    public void setGamePath(Path gamePath){
        this.gamePath = gamePath;
    }

    public List<Integer> getShowedAnnounces(){
        if (announces == null)
            announces = new ArrayList<>();
        return announces;
    }

    public Path getLastBackupPath() { return lastBackupPath; }
    public void setLastBackupPath(Path path){
        this.lastBackupPath = path;
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

    public boolean shouldShowHelloDialog(){
        return showHelloDialog;
    }
    public void setShowHelloDialog(boolean s){
        showHelloDialog = s;
    }

    public boolean shouldPlaceNewProfileToDock(){
        return placeNewProfileToDock;
    }
    public void setPlaceNewProfileToDock(boolean val){
        this.placeNewProfileToDock = val;
    }

    public void setShowOldReleases(boolean showOldReleases) {
        this.showOldReleases = showOldReleases;
    }

    public boolean isShowSnapshots() {
        return showSnapshots;
    }
    public boolean isEnabledAutoUpdate(){
        return autoUpdate;
    }
    public void setAutoUpdate(boolean autoUpdate){
        this.autoUpdate = autoUpdate;
    }

    public boolean delGameLogs(){
        return delGameLogs;
    }

    public void setDelGameLogs(boolean a){
        delGameLogs = a;
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
    public boolean getDebugLogMode(){
        return debugLogMode;
    }
    public void setDebugLogMode(boolean mode){
        debugLogMode = mode;
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

    public void setUser(Account a){
        this.user = a;
    }

    public Account getUser(){
        if (user == null){
            var conf = Configurator.generateDefaultConfig();
            if (conf == null)
                return Account.fromUsername("IAMUSER");
            setUser(conf.getUser());
        }

        return user;
    }

    public Locale getLanguage(){
        return language == null ? Locale.getDefault() : language;
    }

}
