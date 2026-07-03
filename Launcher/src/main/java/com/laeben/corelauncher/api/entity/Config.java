package com.laeben.corelauncher.api.entity;

import com.laeben.corelauncher.api.gpu.entity.GPUType;
import com.laeben.corelauncher.api.ui.entity.UIPreference;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.core.entity.Path;
import com.laeben.corelauncher.util.java.entity.JavaSourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Config {
    private static final Path DEFAULT_GAME_PATH = Path.begin(OSUtil.getAppFolder());

    /* Boolean Fields */

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
    private boolean omitLauncherLibs;
    private boolean useGridAlignment;
    private boolean disableRPC;
    private boolean enableInGameRPC;
    private boolean useNonGuiShortcut;
    private boolean overwriteImported;
    private boolean disableSelectNewProfile;
    private boolean searchBrowserManually;
    private boolean middlePaste;
    private boolean useExternalAuth;
    private boolean useEmbeddedBrowser;

    /* Number Fields */

    private int defaultMinRAM;
    private int defaultMaxRAM;
    private int downloadThreads;
    private int uiScale;
    private double windowWidth;
    private double windowHeight;

    /* Class Fields */

    private Account user;
    private Path gamePath;
    private Path lastBackupPath;
    private Locale language;
    private Path backgroundImage;
    private Profile lastSelectedProfile;
    private List<Java> customJavaVersions;
    private Java defaultJava;
    private JavaSourceType javaSource;
    private List<Integer> announces;
    private List<UIPreference> uiPreferences;
    private String curseApiKey;
    private GPUType gpuType;

    /* Getters */

    public GPUType getGPUType(){
        return gpuType;
    }
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
    public Path getNativesPath(){
        return getLauncherPath().to("natives");
    }
    public List<Integer> getShowedAnnounces(){
        if (announces == null)
            announces = new ArrayList<>();
        return announces;
    }
    public int getUIScale(){
        return uiScale < 1 ? 100 : uiScale;
    }
    public int getDownloadThreadsCount(){
        return downloadThreads == 0 ? 10 : downloadThreads;
    }
    public Path getLastBackupPath() { return lastBackupPath; }
    public boolean omitLauncherLibraries(){
        return omitLauncherLibs;
    }
    public boolean disableSelectNewProfile() {
        return disableSelectNewProfile;
    }
    public JavaSourceType getJavaSourceType() { return javaSource == null ? JavaSourceType.AZUL : javaSource; }
    public Java getDefaultJava(){
        return defaultJava;
    }
    public int getDefaultMinRAM(){
        return defaultMinRAM;
    }
    public String getCurseForgeApiKey(){
        return curseApiKey;
    }
    public boolean doSearchBrowserManually() {
        return searchBrowserManually;
    }
    public boolean useExternalAuth(){
        return useExternalAuth;
    }
    public boolean useEmbeddedBrowser(){
        return useEmbeddedBrowser;
    }
    public boolean useNonGUIShortcut(){
        return useNonGuiShortcut;
    }
    public boolean isAutoChangeLoader(){
        return autoChangeWrapper;
    }
    public boolean isDisabledRPC(){
        return disableRPC;
    }
    public boolean isEnabledInGameRPC(){
        return enableInGameRPC;
    }
    public boolean isEnabledSelectAndPlayDock(){
        return selectAndPlayDock;
    }
    public boolean isOverwriteImportedEnabled(){
        return overwriteImported;
    }
    public boolean isEnabledMiddlePaste(){
        return middlePaste;
    }
    public double getWindowWidth(){
        return windowWidth == 0 ? 1612 : windowWidth;
    }
    public double getWindowHeight(){
        return windowHeight == 0 ? 964 : windowHeight;
    }
    public Path getBackgroundImage(){
        return backgroundImage != null ? (backgroundImage.exists() ? backgroundImage : null) : null;
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
    public boolean shouldPlaceNewProfileToDock(){
        return placeNewProfileToDock;
    }
    public boolean isShowSnapshots() {
        return showSnapshots;
    }
    public boolean isEnabledAutoUpdate(){
        return autoUpdate;
    }
    public boolean delGameLogs(){
        return delGameLogs;
    }
    public boolean getLogMode(){
        return logMode;
    }
    public boolean getDebugLogMode(){
        return debugLogMode;
    }
    public boolean useGridAlignment(){
        return useGridAlignment;
    }
    public boolean hideAfter(){
        return hideAfter;
    }
    public Profile getLastSelectedProfile(){
        return lastSelectedProfile;
    }
    public List<Java> getCustomJavaVersions(){
        if (customJavaVersions == null)
            customJavaVersions = new ArrayList<>();
        return customJavaVersions;
    }
    public UIPreference getUIPreference(String id){
        return getUIPreferences().stream().filter(a -> id.equals(a.getIdentifier())).findFirst().orElse(null);
    }
    public List<UIPreference> getUIPreferences(){
        if (uiPreferences == null)
            uiPreferences = new ArrayList<>();
        return uiPreferences;
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

    /* Setters */

    public void setGPUType(GPUType type){
        this.gpuType = type;
    }
    public void setGamePath(Path gamePath){
        this.gamePath = gamePath;
    }
    public void setUser(Account a){
        this.user = a;
    }
    public void setHideAfter(boolean ha){
        hideAfter = ha;
    }
    public void setUseGridAlignment(boolean val){
        this.useGridAlignment = val;
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
    public void setDebugLogMode(boolean mode){
        debugLogMode = mode;
    }
    public void setBackgroundImage(Path img){
        this.backgroundImage = img;
    }
    public void setShowHelloDialog(boolean s){
        showHelloDialog = s;
    }
    public void setPlaceNewProfileToDock(boolean val){
        this.placeNewProfileToDock = val;
    }
    public void setAutoUpdate(boolean autoUpdate){
        this.autoUpdate = autoUpdate;
    }
    public void setShowOldReleases(boolean showOldReleases) {
        this.showOldReleases = showOldReleases;
    }
    public void setWindowSize(double w, double h){
        windowWidth = w;
        windowHeight = h;
    }
    public void setEnabledMiddlePaste(boolean v){
        this.middlePaste = v;
    }
    public void setUseNonGuiShortcut(boolean val){
        useNonGuiShortcut = val;
    }
    public void setAutoChangeLoader(boolean val){
        autoChangeWrapper = val;
    }
    public void setDisabledRPC(boolean v){
        this.disableRPC = v;
    }
    public void setEnabledInGameRPC(boolean v){
        this.enableInGameRPC = v;
    }
    public void setEnabledSelectAndPlayDock(boolean v){
        this.selectAndPlayDock = v;
    }
    public void setOverwriteImported(boolean v){
        this.overwriteImported = v;
    }
    public void setJavaSourceType(JavaSourceType source){
        this.javaSource = source;
    }
    public void setLanguage(Locale l){
        this.language = l;
    }
    public void setDefaultJava(Java j){
        defaultJava = j;
    }
    public void setDefaultMinRAM(int minRam){
        defaultMinRAM = minRam;
    }
    public void setDefaultMaxRAM(int maxRAM){
        defaultMaxRAM = maxRAM;
    }
    public void setCurseForgeApiKey(String key){
        this.curseApiKey = key;
    }
    public void setUseExternalAuth(boolean val){
        useExternalAuth = val;
    }
    public void setUseEmbeddedBrowser(boolean val){
        useEmbeddedBrowser = val;
    }
    public void setSearchBrowserManually(boolean searchBrowserManually) {
        this.searchBrowserManually = searchBrowserManually;
    }
    public void setUIScale(int uiScale){
        this.uiScale = uiScale;
    }
    public void setDownloadThreadsCount(int downloadThreads){
        this.downloadThreads = downloadThreads;
    }
    public void setLastBackupPath(Path path){
        this.lastBackupPath = path;
    }
    public void setOmitLauncherLibraries(boolean value){
        this.omitLauncherLibs = value;
    }
    public void setDisableSelectNewProfile(boolean disableSelectNewProfile) {
        this.disableSelectNewProfile = disableSelectNewProfile;
    }
}
