package com.laeben.corelauncher.ui.controller;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.Cat;
import com.laeben.core.util.NetUtils;
import com.laeben.core.util.events.*;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.api.*;
import com.laeben.corelauncher.api.entity.FDObject;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.socket.entity.CLPacket;
import com.laeben.corelauncher.api.socket.entity.CLPacketType;
import com.laeben.corelauncher.api.socket.entity.CLStatusPacket;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.ui.entity.FocusLimiter;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.discord.Discord;
import com.laeben.corelauncher.discord.entity.Activity;
import com.laeben.corelauncher.minecraft.Launcher;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.entity.ExecutionInfo;
import com.laeben.corelauncher.minecraft.entity.ServerInfo;
import com.laeben.corelauncher.minecraft.entity.VersionNotFoundException;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.util.ServerHandshake;
import com.laeben.corelauncher.minecraft.loader.Vanilla;
import com.laeben.corelauncher.ui.controller.page.ExtensionsPage;
import com.laeben.corelauncher.ui.controller.page.MainPage;
import com.laeben.corelauncher.ui.controller.page.SettingsPage;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.controller.page.TutorialsPage;
import com.laeben.corelauncher.ui.dialog.DStartupConfigurator;
import com.laeben.corelauncher.ui.entity.EventFilter;
import com.laeben.corelauncher.ui.tutorial.Instructor;
import com.laeben.corelauncher.ui.tutorial.StepPopup;
import com.laeben.corelauncher.ui.util.ControlUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.java.JavaManager;
import com.laeben.corelauncher.wrap.ExtensionWrapper;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main extends HandlerController {
    public static final String KEY = "main";

    public static final String API_TAB_LOAD = "onTabLoad";
    public static final String TAB_FOCUS_CHANGE = "tabFocusChange";
    public static final String TAB_KEY_PRESS = "tabKeyPress";

    public static final String SETTINGS = "settings";
    public static final String ABOUT = "about";
    public static final String FEEDBACK = "fback";
    public static final String EXTENSIONS = "exts";
    public static final String TUTORIALS = "tuts";

    private static Main instance;

    @FXML
    private ProgressBar progress;
    @FXML
    private AnchorPane menu;
    @FXML
    private Pane menuBackground;
    @FXML
    private CView head;
    @FXML
    private TabPane tab;
    @FXML
    private Label lblProfileName;
    @FXML
    private Label lblProfileDescription;
    @FXML
    private VBox menuInner;
    @FXML
    private Label lblStatus;
    @FXML
    private CAnnouncer announcer;
    @FXML
    private Region dialogLayer;
    @FXML
    private AnchorPane root;
    @FXML
    private StepPopup stepPopup;

    @FXML
    private CButton btnPlay;
    private final Image imgPlay;
    private final Image imgPause;

    private Profile selectedProfile;

    private boolean preventScrollFilter;

    private boolean statusNeedsUpdate;
    private double percentage;
    private final ScheduledThreadPoolExecutor statusExecutor;
    private final String[] status;

    private final TranslateTransition menuTranslate;
    private final ScaleTransition prgTranslate;
    private boolean showingProgress;
    private boolean createDefaultProfile;

    private final BooleanProperty running;

    private final Executor executor;

    private FocusLimiter focusLimiter;

    private final Instructor instructor;

    @FXML
    private ProgressIndicator ind;

    public CMenu cMenu;
    private final DecimalFormat df;

    private final EventHandler<KeyEvent> handler;

    public Main(){
        super(KEY);
        df = new DecimalFormat("0.#");
        running = new SimpleBooleanProperty(false);
        running.addListener(a -> UI.runAsync(() -> {
            if (running.get()){
                btnPlay.getStyleClass().add("on");
                btnPlay.getStyleClass().remove("off");
            }
            else{
                btnPlay.getStyleClass().remove("on");
                btnPlay.getStyleClass().add("off");
                setProgress(-1);
                clearStatus();
            }
        }));

        registerHandler(Profiler.getProfiler().getHandler(), a -> {
            var oldProfile = (Profile)a.getOldValue();
            //var oldProfile = (Profile)a.getOldValue();

            switch (a.getKey()) {
                case Profiler.PROFILE_UPDATE -> {
                    if (selectedProfile == a.getNewValue())
                        selectProfile(selectedProfile);
                }
                case Profiler.PROFILE_DELETE -> {
                    if (selectedProfile == oldProfile)
                        selectProfile(null);

                    ImageCacheManager.remove(oldProfile);
                }
                case EventHandler.RELOAD -> selectProfile(null);
            }
        }, true);
        registerHandler(UI.getUI().getHandler(), a -> {
            if (a instanceof KeyEvent ke && ke.getKey().equals(UI.WINDOW_CLOSE)){
                var stage = (Stage)a.getSource();
                if (stage.isMaximized())
                    Configurator.getConfig().setWindowSize(-1, -1);
                Configurator.save();
            }
        }, true);
        registerHandler(JavaManager.getManager().getHandler(), a -> {
            if (!a.getKey().equals(JavaManager.DOWNLOAD_COMPLETE))
                return;

            refreshStates();
        }, true);
        registerHandler(Launcher.getLauncher().getHandler(), this::onGeneralEvent, true);
        registerHandler(Vanilla.getVanilla().getHandler(), this::onGeneralEvent, true);
        registerHandler(Modder.getModder().getHandler(), this::onGeneralEvent, true);
        registerHandler(NetUtil.getHandler(), this::onProgress, false);
        registerHandler(Configurator.getConfigurator().getHandler(), a -> {
            if (a.getKey().equals(Configurator.BACKGROUND_CHANGE))
                setBackground(Configurator.getConfig().getBackgroundImage());
            else if (a.getKey().equals(Configurator.USER_CHANGE)){
                if (selectedProfile != null && selectedProfile.isValid())
                    setUser(selectedProfile.getUser() == null ? Configurator.getConfig().getUser().reload() : selectedProfile.getUser().reload());
                else
                    setUser(Configurator.getConfig().getUser().reload());
            }
        }, true);
        Launcher.getLauncher().setOnAuthFail(v -> {
            var task = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    var msg = CMsgBox.msg(Alert.AlertType.ERROR, Translator.translate("error.oops"), Translator.translateFormat("error.auth", v.getValue()))
                            .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                            .executeForResult();
                    return msg.isPresent() && msg.get().result() != CMsgBox.ResultType.NO;
                }
            };
            Platform.runLater(task);
            try {
                return task.get();
            } catch (InterruptedException | ExecutionException ex) {
                return false;
            }
        });

        menuTranslate = new TranslateTransition();
        prgTranslate = new ScaleTransition();
        menuTranslate.setFromY(0);
        menuTranslate.setToY(-5);
        prgTranslate.setFromY(0);
        prgTranslate.setToY(1);
        var d = Duration.millis(400);
        menuTranslate.setDuration(d);
        prgTranslate.setDuration(d);

        status = new String[2];
        //status = new String[3];

        handler = new EventHandler<>();

        executor = Executors.newSingleThreadExecutor();
        statusExecutor = new ScheduledThreadPoolExecutor(1);
        statusExecutor.scheduleAtFixedRate(() -> {
            if (statusNeedsUpdate){
                UI.runAsync(this::updateStatus);
                statusNeedsUpdate = false;
            }
        },0, 50, TimeUnit.MILLISECONDS);

        instructor = new Instructor();

        imgPlay = ImageCacheManager.getImage("play.png", 48);
        imgPause = ImageCacheManager.getImage("menu.png", 48);

        instance = this;
    }

    /**
     * Revoke stop requests and set running to false.
     */
    public void refreshStates(){
        running.set(false);
        revokeStopRequests();
    }

    public void revokeStopRequests(){
        if (selectedProfile != null)
            selectedProfile.getLoader().setStopRequested(false);
        Vanilla.getVanilla().setStopRequested(false);
        Modder.getModder().setStopRequested(false);
    }

    public void invokeStopRequests(){
        if (selectedProfile != null)
            selectedProfile.getLoader().setStopRequested(true);
        Vanilla.getVanilla().setStopRequested(true);
        Modder.getModder().setStopRequested(true);
        NetUtil.stop();
    }

    /**
     * <h1>Syntax</h1>
     * <h3>Rules</h3>
     * <ul>
     *     <li>Statement must start with dot '.'</li>
     *     <li>All status' separated with dollar '$'</li>
     *     <li>
     *         <p>To specify status priority, status statement has to start with comma (,)</p>
     *         <p>Comma count indicates the status priority (zero comma = primary, one comma = secondary etc.)</p>
     *     </li>
     *     <li>After indicators if there is an exclamation mark (!), value, otherwise translation key is written.</li>
     *     <li>To use formatting, after the translation key or value, semicolon (;) is used to give variables and separate them.</li>
     * </ul>
     * <h3>Definitions</h3>
     * <ul>
     *     <li><code>.[statement]</code></li>
     *     <li><code>.[status1]$[status2]$[status3]...</code></li>
     *     <li>Primary, secondary, trinary status'$ <code>.[status1]$,[status2]$,,,[status3]...</code></li>
     *     <li><code>.![value];[var1];[var2]$,[key2]$,,[key3];[var1]</code></li>
     * </ul>
     * <h1>Example</h1>
     * <h3>Input</h3>
     * <p><code>.,,test.a$test.format.b;value1;value2$,test.format.c;value1</code></p>
     * <h3>Output</h3>
     * <p>Primary Status: translateFormat(test.format.b, value1, value2)</p>
     * <p>Secondary Status: translate(test.a)</p>
     * <p>Trinary Status: translateFormat(test.format.c, value1)</p>
     * @param text input text
     */
    @SuppressWarnings("StringConcatenationInLoop")
    private void processDotFormStatus(String text){
        boolean started = false;
        boolean readingVariable = false;
        boolean isKeyValue = false;

        int commaCount = 0;
        String buffer = "";
        String key = "";
        List<String> variables = new ArrayList<>();

        int len = text.length();

        for(int i = 0; i < len; i++){
            char c = text.charAt(i);

            if (!started){
                started = c == '.';
                continue;
            }

            if (c == '$' || i == len - 1){
                if (i == len - 1)
                    buffer += c;

                if (readingVariable){
                    variables.add(buffer);
                    buffer = "";
                    readingVariable = false;
                }
                else{
                    key = buffer;
                    buffer = "";
                }

                String trns;
                if (isKeyValue)
                    trns = key;
                else if (variables.isEmpty())
                    trns = Translator.translate(key);
                else
                    trns = Translator.translateFormat(key, variables);

                System.out.println(trns);

                if (commaCount == 0)
                    setPrimaryStatus(trns);
                else if (commaCount >= 1)
                    setSecondaryStatus(trns);

                key = "";
                isKeyValue = false;
                commaCount = 0;
                variables.clear();

                continue;
            }

            if (c == ';') {
                if (readingVariable){
                    variables.add(buffer);
                    buffer = "";
                }
                else {
                    key = buffer;
                    buffer = "";
                    readingVariable = true;
                }
            }
            else if (c == ',')
                commaCount++;
            else if (c == '!' && !readingVariable && buffer.isEmpty()){
                isKeyValue = true;
            }
            else {
                buffer += c;
            }
        }

        /*if (key.startsWith("."))
            setPrimaryStatus(processDotFormStatus(key.substring(1)));
        else if (key.startsWith(",")){
            var s = key.split(":\\.");
            setSecondaryStatus(processDotFormStatus(s[1]));
            setPrimaryStatus(s[0].substring(1));
        }

        String[] spl = f.split(";");
        return spl.length == 1 ? Translator.translate(spl[0]) : Translator.translateFormat(spl[0], Arrays.stream(spl).skip(1).map(x -> (Object) x).toList());*/
    }

    public TabPane getTab(){
        return tab;
    }

    public Instructor getInstructor(){
        return instructor;
    }

    public CAnnouncer getAnnouncer(){
        return announcer;
    }

    public void announceLater(String title, String content, Announcement.AnnouncementType type, Duration d){
        UI.runAsync(() -> announcer.announce(new Announcement(title, content, type), d));
    }

    public boolean announceLater(Throwable ex, Duration d){
        String msg;

        boolean state = true;

        if (ex instanceof StopException)
            return state;
        else if (state = ex instanceof NoConnectionException)
            msg = Translator.translate("error.connection");
        else
            msg = Translator.translate("error.unknown");
        UI.runAsync(() -> announcer.announce(new Announcement(Translator.translate("error.oops"), msg, Announcement.AnnouncementType.ERROR), d));
        return state;
    }

    private void onGeneralEvent(BaseEvent e) {
        if (e instanceof ProgressEvent p)
            onProgress(p);
        else if (e instanceof ValueEvent v){
            if (v.getKey().startsWith(Launcher.SESSION_END)){
                announcer.announce(new Announcement(Translator.translate("announce.game.ended"), Translator.translateFormat("announce.misc.profile", v.getKey().substring(10)) + "\n" + Translator.translateFormat("announce.misc.ecode", v.getValue()), Announcement.AnnouncementType.GAME), Duration.seconds(3));
                Discord.getDiscord().setActivity(Activity.setForIdling());
                if (Configurator.getConfig().hideAfter())
                    UI.getUI().showAll();
            }
            else if (v.getKey().equals(Launcher.SESSION_RECEIVE)){
                var val = (CLPacket)v.getValue();
                if (val.getType() != CLPacketType.STATUS || !Configurator.getConfig().isEnabledInGameRPC())
                    return;
                var status = new CLStatusPacket(val);
                if (status.getType() == CLStatusPacket.InGameType.MULTIPLAYER)
                    executor.execute(() -> {
                        var a = ServerHandshake.shake(status.getData(), 25565);
                        if (a != null)
                            Discord.getDiscord().setActivity(Activity.setForParty("abc", a.players(), a.maxPlayers()));
                    });
                Discord.getDiscord().setActivity(Activity.setForInGame(status));
            }
        }
        else if (e instanceof KeyEvent k){
            var key = k.getKey();

            if (key.charAt(0) == '.'){
                System.out.println(key);
                processDotFormStatus(key);
                return;
            }

            if (key.startsWith(Launcher.SESSION_START)){
                announcer.announce(new Announcement(Translator.translate("announce.game.started"), Translator.translateFormat("announce.misc.profile", k.getKey().substring(Launcher.SESSION_START.length())), Announcement.AnnouncementType.GAME), Duration.seconds(3));
                refreshStates();
                Discord.getDiscord().setActivity(Activity.setForProfile(selectedProfile));
                if (Configurator.getConfig().hideAfter())
                    UI.getUI().hideAll();
            }
            else if (key.equals(Launcher.JAVA_DOWNLOAD_ERROR)){
                setPrimaryStatus(Translator.translate("error.launch.java"));
            }
            else if (key.startsWith(Launcher.JAVA)){
                String major = k.getKey().substring(Launcher.JAVA.length());
                setPrimaryStatus(Translator.translateFormat("launch.state.download.java", major));
            }
            else if (key.equals(EventHandler.STOP)){
                refreshStates();
            }
            else if (key.equals(Loader.CLIENT_DOWNLOAD))
                setPrimaryStatus(Translator.translate("launch.state.download.client"));
            /*else if (key.startsWith(Loader.LIBRARY)){
                setPrimaryStatus(key.substring(3));
            }
            else if (key.startsWith(Loader.ASSET)){//
                setPrimaryStatus(Translator.translate("launch.state.download.assets") + " " + key.substring(5));
            }*/
            else if (key.equals(Loader.ASSET_LOAD)){
                setPrimaryStatus(Translator.translate("launch.state.download.assets"));
            }
            else if (key.equals(Loader.LIBRARY_LOAD)){
                setPrimaryStatus(Translator.translate("launch.state.download.libraries"));
            }
            else if (key.equals(Loader.LAUNCH_FINISH)){
                setPrimaryStatus(Translator.translate("launch.state.finish"));
            }
            else if (key.equals(Loader.UNKNOWN_ERROR)){
                setPrimaryStatus(Translator.translate("error.unknown"));
            }
            else if (key.startsWith(Loader.ACQUIRE_VERSION))
                setPrimaryStatus(Translator.translateFormat("launch.state.acquire", key.substring(Loader.ACQUIRE_VERSION.length())));
            else if (key.startsWith(Launcher.PREPARE)){
                setPrimaryStatus(Translator.translateFormat("launch.state.prepare", key.substring(Launcher.PREPARE.length())));
                Discord.getDiscord().setActivity(a -> a.state = Translator.translate("discord.state.prepare"));
            }
            else if (key.startsWith(Modder.RESOURCE_INSTALL)){
                setPrimaryStatus(Translator.translateFormat("resource.install", key.substring(Modder.RESOURCE_INSTALL.length())));
            }
            else
                setPrimaryStatus(key);
        }
    }

    public static Main getMain(){
        return instance;
    }

    public void selectProfile(Profile p){
        selectedProfile = p;

        UI.runAsync(() -> {
            try{
                if (p != null && p.isValid()){
                    lblProfileName.setText(p.getName());
                    lblProfileDescription.setText(p.getVersionId() + " " + StrUtil.toUpperFirst(p.getLoader().getType().getIdentifier()));
                    setUser(p.getUser() == null ? Configurator.getConfig().getUser().reload() : p.getUser().reload());
                }
                else{
                    lblProfileName.setText(null);
                    lblProfileDescription.setText(null);
                    setUser(Configurator.getConfig().getUser().reload());
                }

                Configurator.getConfig().setLastSelectedProfile(p);
                Configurator.save();
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

    }
    public Profile getSelectedProfile(){
        return selectedProfile;
    }

    private void setUser(Account account) {
        head.setImage(account.getHead());
        Tooltip.install(head, new Tooltip(account.getUsername()));
    }

    public Bounds getTabBounds(){
        return tab.localToScreen(tab.getBoundsInLocal());
    }

    @Override
    public void preInit(){
        var btnMenu = new CButton();
        btnMenu.getStyleClass().add("menu-button");
        btnMenu.setId("btnMenu");
        btnMenu.setPrefHeight(64);
        btnMenu.setPrefWidth(64);
        btnMenu.setOnMouseClicked(a -> cMenu.show());
        cMenu.setButton(btnMenu);

        menuTranslate.setNode(menuInner);
        prgTranslate.setNode(progress);

        cMenu.addItem(null, SETTINGS, Translator.translate("settings"), a -> addTab("pages/settings", Translator.translate("settings"), true, SettingsPage.class));
        cMenu.addItem(null, ABOUT, Translator.translate("about"), a -> CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("about.title"), Translator.translateFormat("about.content", LauncherConfig.VERSION, "https://github.com/etkmlm/CoreLauncher", "https://github.com/etkmlm", "https://discord.gg/MEJQtCvwqf", LauncherConfig.APPLICATION.getName())).execute());
        cMenu.addItem(null, FEEDBACK, Translator.translate("feedback"), a -> {
            try {
                OSUtil.openURL("https://github.com/etkmlm/CoreLauncher/issues");
            } catch (IOException ignored) {

            }
        });
        cMenu.addItem(null, EXTENSIONS, Translator.translate("extensions"), a -> addTab("pages/extensions", Translator.translate("extensions"), true, ExtensionsPage.class));
        cMenu.addItem(null, TUTORIALS, Translator.translate("tutorial.tutorials"), a -> {
            /*instructor.load(Instructor.generateGeneralTutorial());
            instructor.start();*/
            addTab("pages/tutorials", Translator.translate("tutorial.tutorials"), true, TutorialsPage.class);
        });

        try{
            setUser(Configurator.getConfig().getUser().reload());

            if (Configurator.getConfig().getLastSelectedProfile() != null){
                var selected = Profiler.getProfiler().getProfile(Configurator.getConfig().getLastSelectedProfile().getName());
                selectProfile(selected);
            }
            else
                selectProfile(null);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        tab.setOnKeyPressed(a -> handler.execute((KeyEvent) new KeyEvent(TAB_KEY_PRESS).setSource(a)));
        tab.getSelectionModel().selectedItemProperty().addListener((a, o, n) -> handler.execute((KeyEvent) new KeyEvent(TAB_FOCUS_CHANGE).setSource(n)));

        btnPlay.setOnMouseClicked(a -> launchClick(a.isShiftDown()));

        head.setCornerRadius(64, 64, 16);

        instructor.setPopup(stepPopup);
    }

    public void setFocusLimiter(FocusLimiter limit){
        focusLimiter = limit;
    }

    public static AnchorPane getExternalLayer(CTab tab){
        var c = new AnchorPane();
        var f = (ScrollPane)tab.getContent();
        c.getChildren().add(f);
        ControlUtil.setAnchorFill(f);

        return c;
    }

    private void setBackground(Path path) {
        try {
            var background = new Background(new BackgroundImage(
                    new Image(path.toFile().toURI().toURL().toExternalForm()),
                    null,
                    null,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(root.getWidth(),root.getHeight(), false, false, true, true)));
            double r = 10;
            double w = 1300;
            double h = 800;
            root.setStyle("-fx-shape:\"M " + r + " 0 " + " L " + (w - r) + " " + "0" + " Q " + w + " 0 " + w + " " + r + " L " + w + " " + (h - r) + " Q " + w + " " + h + " " + (w - r) + " " + h + " L " + r + " " + h + " Q 0 " + h + " 0 " + (h - r) + " L 0 " + r + " Q 0 0 " + r + " 0 Z\"");
            root.setBackground(background);
        } catch (Exception e) {
            root.setBackground(null);
            root.setStyle(null);
        }
    }

    @Override
    public void init() {
        addRegisteredEventFilter(EventFilter.node(root, MouseEvent.ANY, (a) -> {
            if (focusLimiter == null)
                return;

            var node = (Node) a.getTarget();

            if (focusLimiter.verify(a.getSceneX(), a.getSceneY()))
                return;

            if (a.getEventType() == MouseEvent.MOUSE_CLICKED){
                focusLimiter.focus();
                focusLimiter.onFocusLimitIgnored(Tool.findNodeController(node), node);
            }

            a.consume();
        }));

        addRegisteredEventFilter(EventFilter.window(getStage(), javafx.scene.input.KeyEvent.KEY_PRESSED, x -> {
            if (!x.isShiftDown() || x.getTarget() instanceof TextField)
                return;

            if (x.getCode() == KeyCode.LEFT){
                var selTab = tab.getSelectionModel().getSelectedItem();
                int i1 = tab.getTabs().indexOf(selTab);
                if (i1 <= 1)
                    return;
                relocateTab(i1, i1 - 1);
            }
            else if (x.getCode() == KeyCode.RIGHT){
                var selTab = tab.getSelectionModel().getSelectedItem();
                int i1 = tab.getTabs().indexOf(selTab);
                if (i1 == 0)
                    return;
                relocateTab(i1, i1 + 1);
            }
        }));

        menu.boundsInLocalProperty().addListener((a, ob, bounds) -> {
            var n = new Rectangle(0, 0, bounds.getWidth(), bounds.getHeight());
            n.setArcWidth(32);
            n.setArcHeight(32);
            menuBackground.setClip(n);
        });

        addTab("pages/main", "        ", false, MainPage.class);

        setBackground(Configurator.getConfig().getBackgroundImage());

        if (Configurator.getConfig().shouldShowHelloDialog()){
            var x = new DStartupConfigurator().execute();
            if (x){
                Configurator.getConfig().setShowHelloDialog(false);
                Configurator.save();
            }

            createDefaultProfile = true;
        }

        double w = Configurator.getConfig().getWindowWidth();
        double h = Configurator.getConfig().getWindowHeight();
        if (w > 0 && h > 0){
            getStage().setWidth(w);
            getStage().setHeight(h);
        }
        else if (w == -1 && h == -1){
            getStage().setMaximized(true);
        }
    }

    /* TABS */
    private ScrollPane getScroll(){
        var pane = new ScrollPane();
        pane.getStyleClass().add("main-scroll");
        pane.setStyle("-fx-background-color: -tab-fill; -fx-border-radius: 0 16px 16px 16px;-fx-background-radius: 0 16px 16px 16px;");
        pane.setFitToWidth(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setFitToHeight(true);
        addRegisteredEventFilter(EventFilter.node(pane, ScrollEvent.SCROLL, a -> {
            if (preventScrollFilter)
                return;

            double val = a.getDeltaY() * 0.001 * (pane.getHeight() * 1 / 800);
            pane.setVvalue(pane.getVvalue() - val);
        }));
        return pane;
    }
    public void closeTab(int index){
        var t = tab.getTabs().get(index);
        if (t instanceof CTab ct){
            removeRegisteredEventFilter(ct.getContent());
            ct.dispose();
        }
        tab.getTabs().remove(t);
    }
    public void closeTab(Tab tab){
        if (tab instanceof CTab ct){
            removeRegisteredEventFilter(ct.getContent());
            ct.dispose();
        }
        getTab().getTabs().remove(tab);
    }
    public void relocateTab(int i1, int i2){
        int limit = tab.getTabs().size() - 1;
        if (i2 > limit || i1 > limit)
            return;
        var tabs = new ArrayList<>(tab.getTabs());
        Collections.swap(tabs, i1, i2);
        tab.getTabs().setAll(tabs);
    }

    /**
     * Replaces the old tab with the new one.
     * @param from old controller
     * @param fxml target layout path
     * @param title target title
     * @param closable is the new tab closable
     * @param type type instance
     * @return the controller of the new tab
     * @param <T> type of the new controller
     */
    public <T extends Controller> T replaceTab(Controller from, String fxml, String title, boolean closable, Class<T> type){
        var t = tab.getTabs().stream().filter(x -> x instanceof CTab ct && from.equals(ct.getController())).findFirst();
        int index = t.map(tab.getTabs()::indexOf).orElse(-1);
        if (index != -1)
            closeTab(index);

        var t1 = createTab(fxml, title, closable);

        tab.getTabs().add(index, t1);
        tab.getSelectionModel().select(t1);

        //handler.execute((KeyEvent) new KeyEvent(TAB_FOCUS_CHANGE).setSource(t1));

        ExtensionWrapper.getWrapper().fireEvent(API_TAB_LOAD, t1);

        return (T)t1.getController();
    }

    /**
     * Creates a tab, and adds it to the pane.
     * @param fxml target layout path
     * @param title target title
     * @param closable is the tab closable
     * @param type type instance
     * @return controller
     * @param <T> type of the controller
     */
    public <T extends Controller> T addTab(String fxml, String title, boolean closable, Class<T> type){
        var t = tab.getTabs().stream().filter(x -> x.getText().equals(title)).findFirst().orElse(null);

        if (t instanceof CTab ct){
            tab.getSelectionModel().select(ct);
            return (T)ct.getController();
        }

        var t1 = createTab(fxml, title, closable);

        tab.getTabs().add(t1);

        tab.getSelectionModel().select(t1);
        t1.getController().onShown();

        //handler.execute((KeyEvent) new KeyEvent(TAB_FOCUS_CHANGE).setSource(t1));

        ExtensionWrapper.getWrapper().fireEvent(API_TAB_LOAD, t1);

        return (T)t1.getController();
    }

    private CTab createTab(String fxml, String title, boolean closable){
        var t = new CTab();
        t.setClosable(closable);
        t.setText(title);

        var n = UI.load(CoreLauncherFX.class.getResource("layout/" + fxml + ".fxml"));

        var scroll = getScroll();
        scroll.setContent(n.getRootNode());
        t.setContent(scroll);
        t.setOnClosed(a -> {
            removeRegisteredEventFilter(scroll);
            t.dispose();
        });
        t.setController(n.setNode(scroll).setParentObject(t).setStage(getStage()));

        return t;
    }

    /* TABS END */

    private void onProgress(ProgressEvent e){
        if (!running.get())
            running.set(true);
        String id = e.getKey().equals(NetUtils.DOWNLOAD) ? "mb" : e.getKey();
        setSecondaryStatus(df.format(e.getRemain()) + id + " / " + df.format(e.getTotal()) + id);

        setProgress(e.getProgress());
    }
    public void setProgress(double progress){
        if (!showingProgress){
            showProgress();
        }
        if (progress < 0){
            hideProgress();
            progress = 0;
        }


        percentage = progress;
        statusNeedsUpdate = true;
        //this.progress.setProgress(progress);
    }
    public void showProgress(){
        this.progress.setProgress(0);
        menuTranslate.playFromStart();
        prgTranslate.playFromStart();
        showingProgress = true;
    }
    public void hideProgress(){
        menuTranslate.playFromStart();
        prgTranslate.playFromStart();
        menuTranslate.jumpTo(Duration.ZERO);
        prgTranslate.jumpTo(Duration.ZERO);
        menuTranslate.stop();
        prgTranslate.stop();
        showingProgress = false;
    }

    /**
     * Clears the status object.
     */
    public void clearStatus(){
        Arrays.fill(status, null);
        lblStatus.setText(null);

        setProgress(-1);
    }

    /**
     * Triggers the update of the status.
     */
    public void updateStatus(){
        if (!running.get())
            return;

        String s1 = status[0];
        String s2 = status[1];

        String stat = null;
        if (s1 != null && !s1.isBlank())
            stat = s1;

        if (s2 != null && !s2.isBlank())
            stat = stat == null ? s2 : stat + " | " + s2;

        lblStatus.setText(stat);
        progress.setProgress(percentage);
    }

    /**
     * Sets the primary status.
     * @param text Status text.
     */
    public void setPrimaryStatus(String text){
        status[0] = text;
        statusNeedsUpdate = true;
        //updateStatus();
    }

    /**
     * Sets the secondary status.
     * @param text Status text.
     */
    public void setSecondaryStatus(String text){
        status[1] = text;
        statusNeedsUpdate = true;
        //updateStatus();
    }

    public EventHandler<KeyEvent> getHandler(){
        return handler;
    }

    public boolean launchClick(boolean cache){
        if (running.get()){
            if (selectedProfile != null) {
                invokeStopRequests();
                Discord.getDiscord().setActivity(Activity.setForIdling());
            }
            running.set(false);
            return false;
        }
        else if (selectedProfile != null){
            launch(selectedProfile, cache, null);
            return true;
        }
        return false;
    }

    /*
    * revoke stop requests
    * - prepare
    * * throw stop exception
    * clear status
    * - launch (includes start and end session event)
    * enable caches and remove handler
    *
    * in case of exception
    * refresh states
    * show ui
    * enable caches and remove handler
    *
    * session start
    * refresh states
    * hide ui
    *
    * session end
    * show ui
    * */
    public void launch(Profile p, boolean cache, ServerInfo server){
        var wr = (Loader<?>)p.getLoader();
        Vanilla.getVanilla().setDisableCache(cache);
        Modder.getModder().setDisableCache(cache);
        wr.setDisableCache(cache);
        wr.getHandler().addHandler(KEY, this::onGeneralEvent, true);

        var task = new Task<>() {
            @Override
            protected Object call() throws NoConnectionException, StopException, HttpException, FileNotFoundException, PerformException, VersionNotFoundException {
                revokeStopRequests();

                Launcher.getLauncher().prepare(p);
                Cat.sleep(200);

                if (wr.isStopRequested())
                    throw new StopException();

                UI.runAsync(Main.this::clearStatus);

                // includes session start event
                // announce -> set discord activity -> hide ui

                // includes session end event
                // announce -> set discord activity -> show ui
                Launcher.getLauncher().launch(ExecutionInfo.fromProfile(p).includeServer(server));

                Cat.sleep(200);

                wr.setDisableCache(false);
                Vanilla.getVanilla().setDisableCache(false);
                Modder.getModder().setDisableCache(false);
                wr.getHandler().removeHandler(KEY);

                return null;
            }
        };

        task.setOnFailed(a -> {
            var f = a.getSource().getException();

            Cat.sleep(500);

            // reset them again because it failed
            UI.runAsync(this::refreshStates);
            if (Configurator.getConfig().hideAfter())
                UI.getUI().showAll();
            wr.setDisableCache(false);
            wr.getHandler().removeHandler(KEY);
            Vanilla.getVanilla().setDisableCache(false);
            Modder.getModder().setDisableCache(false);
            // ---

            if (f instanceof NoConnectionException){
                announceLater(Translator.translate("error.oops"),Translator.translate("error.connection"), Announcement.AnnouncementType.ERROR, Duration.millis(3000));
                //UI.runAsync(() -> setPrimaryStatus());
            }
            else if (f instanceof VersionNotFoundException e){
                announceLater(Translator.translate("error.oops"), Translator.translateFormat("error.noVersion", e.getMessage()), Announcement.AnnouncementType.ERROR, Duration.millis(3000));
                //UI.runAsync(() -> setPrimaryStatus());
            }
            else if (f instanceof StopException){
                //
            }
            else if (f instanceof PerformException pe){
                announceLater(Translator.translate("error.oops"), pe.getMessage(), Announcement.AnnouncementType.ERROR, Duration.millis(3000));
                //UI.runAsync(() -> setPrimaryStatus(pe.getMessage()));
            }
            else if (f instanceof Exception e){
                Logger.getLogger().log(e);
                announceLater(Translator.translate("error.unknown"),e.getMessage(), Announcement.AnnouncementType.ERROR, Duration.millis(4000));
            }
        });

        running.set(true);

        new Thread(task).start();
    }

    public void setDialogLayer(boolean v){
        dialogLayer.setVisible(v);
    }

    public void setPreventScrollFilter(boolean value){
        this.preventScrollFilter = value;
    }

    @Override
    public void onShown() {
        var path = Configurator.getConfig().getGamePath().toString();
        if (!Tool.checkStringValidity(path, Tool.ValidityDegree.HIGH_PATH))
            announceLater(Translator.translate("announce.warn"), Translator.translate("settings.warn.invalidGamePath"), Announcement.AnnouncementType.INFO, Duration.millis(8000));

        instructor.setBaseNode(getRootNode());

        if (!createDefaultProfile)
            return;

        createDefaultProfile = false;

        UI.runAsync(() -> {
            var p = Profiler.getProfiler().generateDefaultProfile();
            var size = tab.getBoundsInLocal();
            var tab1 = (Region)tab.getTabs().get(0).getContent();
            var obj = FDObject.createSingle(p, tab1.getWidth() / 2 - 64, tab1.getHeight() / 2 - 64);
            FloatDock.getDock().place(obj, false);
            selectProfile(p);
        });

        instructor.load(Instructor.generateGeneralTutorial());
        instructor.start(false);
    }

    @Override
    public void dispose(){
        statusExecutor.shutdown();
        stepPopup.dispose();
        Launcher.getLauncher().setOnAuthFail(null);
        super.dispose();
    }
}
