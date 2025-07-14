package com.laeben.corelauncher.api.entity;

import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.core.entity.Path;
import com.laeben.corelauncher.util.entity.JavaSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Config {
    private static final Path DEFAULT_GAME_PATH = Path.begin(OSUtil.getAppFolder());
    //private static final int DEFAULT_COMM_PORT = 9875;

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
    private int downloadThreads;
    private int uiScale;
    private boolean showOldReleases;
    private boolean showSnapshots;
    private boolean logMode;
    private boolean debugLogMode;
    private boolean hideAfter;
    private boolean autoUpdate;
    private boolean delGameLogs;
    private boolean showHelloDialog;
    private boolean placeNewProfileToDock;
    private boolean selectAndPlayDock;
    private boolean autoChangeWrapper;
    private JavaSource  javaSource;
    private List<Integer> announces;

    private double windowWidth;
    private double windowHeight;

    private boolean disableRPC;
    //private int commPort;
    private boolean enableInGameRPC;
    private boolean useNonGuiShortcut;
    private boolean overwriteImported;

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

    public int getUIScale(){
        return uiScale < 1 ? 100 : uiScale;
    }
    public void setUIScale(int uiScale){
        this.uiScale = uiScale;
    }

    public int getDownloadThreadsCount(){
        return downloadThreads == 0 ? 10 : downloadThreads;
    }
    public void setDownloadThreadsCount(int downloadThreads){
        this.downloadThreads = downloadThreads;
    }

    public Path getLastBackupPath() { return lastBackupPath; }
    public void setLastBackupPath(Path path){
        this.lastBackupPath = path;
    }

    public JavaSource getJavaSource() { return javaSource == null ? JavaSource.AZUL : javaSource; }
    public void setJavaSource(JavaSource source){
        this.javaSource = source;
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

    public boolean useNonGUIShortcut(){
        return useNonGuiShortcut;
    }
    public void setUseNonGuiShortcut(boolean val){
        useNonGuiShortcut = val;
    }

    public boolean isAutoChangeLoader(){
        return autoChangeWrapper;
    }
    public void setAutoChangeLoader(boolean val){
        autoChangeWrapper = val;
    }

    public boolean isDisabledRPC(){
        return disableRPC;
    }
    public void setDisabledRPC(boolean v){
        this.disableRPC = v;
    }

    public boolean isEnabledInGameRPC(){
        return enableInGameRPC;
    }
    public void setEnabledInGameRPC(boolean v){
        this.enableInGameRPC = v;
    }

    public boolean isEnabledSelectAndPlayDock(){
        return selectAndPlayDock;
    }
    public void setEnabledSelectAndPlayDock(boolean v){
        this.selectAndPlayDock = v;
    }

    public boolean isOverwriteImportedEnabled(){
        return overwriteImported;
    }
    public void setOverwriteImported(boolean v){
        this.overwriteImported = v;
    }

    public double getWindowWidth(){
        return windowWidth == 0 ? 1612 : windowWidth;
    }

    public double getWindowHeight(){
        return windowHeight == 0 ? 964 : windowHeight;
    }

    public void setWindowSize(double w, double h){
        windowWidth = w;
        windowHeight = h;
    }

    /*public int getCommPort(){
        return commPort <= 0 ? DEFAULT_COMM_PORT : commPort;
    }

    public void setCommPort(int port){
        this.commPort = port;
    }*/

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
