package com.laeben.corelauncher.ui.controller;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.Cat;
import com.laeben.core.util.events.*;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.api.entity.Config;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.socket.entity.CLPacket;
import com.laeben.corelauncher.api.socket.entity.CLPacketType;
import com.laeben.corelauncher.api.socket.entity.CLStatusPacket;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.discord.Discord;
import com.laeben.corelauncher.discord.entity.Activity;
import com.laeben.corelauncher.minecraft.Launcher;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.entity.ExecutionInfo;
import com.laeben.corelauncher.minecraft.entity.ServerInfo;
import com.laeben.corelauncher.minecraft.entity.VersionNotFoundException;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.util.ServerHandshake;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.ui.controller.page.MainPage;
import com.laeben.corelauncher.ui.controller.page.SettingsPage;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.dialog.DStartupConfigurator;
import com.laeben.corelauncher.ui.util.ControlUtil;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.util.ImageCacheManager;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main extends HandlerController {

    private static Main instance;

    @FXML
    private ProgressBar progress;
    @FXML
    private AnchorPane menu;
    @FXML
    private CView head;
    @FXML
    private TabPane tab;
    @FXML
    private Label lblProfileName;
    @FXML
    private Label lblProfileDescription;
    @FXML
    private CButton btnPlay;
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

    private Profile selectedProfile;
    private final String[] status;

    private final TranslateTransition menuTranslate;
    private final ScaleTransition prgTranslate;
    private boolean showingProgress;

    private final BooleanProperty running;

    private final Executor executor;

    @FXML
    private ProgressIndicator ind;

    public CMenu cMenu;
    private final DecimalFormat df;

    private final EventHandler<KeyEvent> handler;

    public Main(){
        super("main");
        df = new DecimalFormat("0.#");
        running = new SimpleBooleanProperty(false);
        running.addListener(a -> {
            Platform.runLater(() -> {
                btnPlay.setText(running.get() ? "⏸" : "⯈");
                if (!running.get()){
                    setProgress(-1);
                    clearStatus();
                }
            });
        });

        registerHandler(Profiler.getProfiler().getHandler(), a -> {
            var oldProfile = (Profile)a.getOldValue();
            //var oldProfile = (Profile)a.getOldValue();

            if (a.getKey().equals("profileUpdate")){
                if (selectedProfile == a.getNewValue())
                    selectProfile(selectedProfile);
            }
            else if (a.getKey().equals("profileDelete")){
                if (selectedProfile == oldProfile)
                    selectProfile(null);

                ImageCacheManager.remove(oldProfile);
            }
            else if (a.getKey().equals("reload"))
                selectProfile(null);
        }, true);
        registerHandler(UI.getUI().getHandler(), a -> {
            if (a instanceof KeyEvent ke && ke.getKey().equals("windowClose")){
                var stage = (Stage)a.getSource();
                if (stage.isMaximized())
                    Configurator.getConfig().setWindowSize(-1, -1);
                else
                    Configurator.getConfig().setWindowSize(stage.getWidth(), stage.getHeight());
                Configurator.save();
            }
        }, true);
        registerHandler(Launcher.getLauncher().getHandler(), this::onGeneralEvent, true);
        registerHandler(Vanilla.getVanilla().getHandler(), this::onGeneralEvent, true);
        registerHandler(Modder.getModder().getHandler(), this::onGeneralEvent, true);
        registerHandler(NetUtil.getHandler(), this::onProgress, true);
        registerHandler(Configurator.getConfigurator().getHandler(), a -> {
            if (a.getKey().equals("bgChange"))
                setBackground(Configurator.getConfig().getBackgroundImage());
            else if (a.getKey().equals("userChange")){
                if (selectedProfile != null && selectedProfile.isValid())
                    setUser(selectedProfile.getUser() == null ? Configurator.getConfig().getUser().reload() : selectedProfile.getUser().reload());
                else
                    setUser(Configurator.getConfig().getUser().reload());
            }
        }, true);

        menuTranslate = new TranslateTransition();
        prgTranslate = new ScaleTransition();
        menuTranslate.setFromY(0);
        menuTranslate.setToY(-5);
        prgTranslate.setFromY(0);
        prgTranslate.setToY(1);
        var d = Duration.millis(400);
        menuTranslate.setDuration(d);
        prgTranslate.setDuration(d);

        status = new String[3];

        handler = new EventHandler<>();

        executor = Executors.newSingleThreadExecutor();

        instance = this;
    }

    private String dot(String f){
        String[] spl = f.split(";");
        return spl.length == 1 ? Translator.translate(spl[0]) : Translator.getTranslator().getTranslateFormat(spl[0], Arrays.stream(spl).skip(1).map(x -> (Object) x).toList());
    }

    public TabPane getTab(){
        return tab;
    }

    public CAnnouncer getAnnouncer(){
        return announcer;
    }

    public void announceLater(String title, String content, Announcement.AnnouncementType type, Duration d){
        Platform.runLater(() -> announcer.announce(new Announcement(title, content, type), d));
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
        Platform.runLater(() -> announcer.announce(new Announcement(Translator.translate("error.oops"), msg, Announcement.AnnouncementType.ERROR), d));
        return state;
    }

    private void onGeneralEvent(BaseEvent e) {
        if (e instanceof ProgressEvent p)
            onProgress(p);
        else if (e instanceof ValueEvent v){
            if (v.getKey().startsWith("sessionEnd")){
                announcer.announce(new Announcement(Translator.translate("announce.game.ended"), Translator.translateFormat("announce.misc.profile", v.getKey().substring(10)) + "\n" + Translator.translateFormat("announce.misc.ecode", v.getValue()), Announcement.AnnouncementType.GAME), Duration.seconds(3));
                Discord.getDiscord().setActivity(Activity.setForIdling());
            }
            else if (v.getKey().equals("sessionReceive")){
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

            if (key.startsWith("sessionStart")){
                announcer.announce(new Announcement(Translator.translate("announce.game.started"), Translator.translateFormat("announce.misc.profile", k.getKey().substring(12)), Announcement.AnnouncementType.GAME), Duration.seconds(3));
                running.set(false);
                Discord.getDiscord().setActivity(Activity.setForProfile(selectedProfile));
                if (Configurator.getConfig().hideAfter())
                    UI.getUI().hideAll();
            }
            else if (key.startsWith("java")){
                String major = k.getKey().substring(4);
                setStatus(1, Translator.translateFormat("launch.state.download.java", major));
            }
            else if (key.equals("stop")){
                running.set(false);
            }
            else if (key.equals("clientDownload"))
                setStatus(1, Translator.translate("launch.state.download.client"));
            else if (key.startsWith("lib")){
                setStatus(1, key.substring(3));
            }
            else if (key.startsWith("asset")){
                setStatus(1, Translator.translate("launch.state.download.assets") + " " + key.substring(5));
            }
            else if (key.startsWith("acqVersion"))
                setStatus(1, Translator.translateFormat("launch.state.acquire", key.substring(10)));
            else if (key.startsWith("prepare")){
                setStatus(1, Translator.translateFormat("launch.state.prepare", key.substring(7)));
                Discord.getDiscord().setActivity(a -> a.state = Translator.translate("discord.state.prepare"));
            }
            else if (key.startsWith("."))
                setStatus(1, dot(key.substring(1)));
            else if (key.startsWith(",")){
                var s = key.split(":\\.");
                setStatus(0, dot(s[1]));
                setStatus(1, s[0].substring(1));
            }
            /*else if (key.equals("jvdown")){

            }*/
            else
                setStatus(1, key);
        }
    }

    public static Main getMain(){
        return instance;
    }

    public void selectProfile(Profile p){
        selectedProfile = p;

        Platform.runLater(() -> {
            try{
                if (p != null && p.isValid()){
                    lblProfileName.setText(p.getName());
                    lblProfileDescription.setText(p.getVersionId() + " " + StrUtil.toUpperFirst(p.getWrapper().getType().getIdentifier()));
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
        btnMenu.setText("☰");
        btnMenu.setPrefHeight(64);
        btnMenu.setPrefWidth(64);
        btnMenu.setOnMouseClicked(a -> cMenu.show());
        cMenu.setButton(btnMenu);

        menuTranslate.setNode(menuInner);
        prgTranslate.setNode(progress);

        cMenu.addItem(null, Translator.translate("settings"), a -> addTab("pages/settings", Translator.translate("settings"), true, SettingsPage.class));
        cMenu.addItem(null, Translator.translate("about"), a -> CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("about.title"), Translator.translateFormat("about.content", LauncherConfig.VERSION, "https://github.com/etkmlm", "https://discord.gg/MEJQtCvwqf", LauncherConfig.APPLICATION.getName())).execute());
        cMenu.addItem(null, Translator.translate("feedback"), a -> {
            try {
                OSUtil.openURL("https://github.com/etkmlm/CoreLauncher/issues");
            } catch (IOException ignored) {

            }
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

        tab.setOnKeyPressed(a -> handler.execute((KeyEvent) new KeyEvent("key").setSource(a)));

        btnPlay.setOnMouseClicked(a -> {
            if (running.get()){
                if (selectedProfile != null) {
                    selectedProfile.getWrapper().setStopRequested(true);
                    Vanilla.getVanilla().setStopRequested(true);
                    NetUtil.stop();
                    Discord.getDiscord().setActivity(Activity.setForIdling());
                }
                running.set(false);
            }
            else if (selectedProfile != null){
                launch(selectedProfile, a.isShiftDown(), null);
            }
        });

        head.setCornerRadius(64, 64, 16);
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
        var scene = getStage().getLScene();
        tab.setOnMousePressed(a -> {
            if (a.getTarget() instanceof StackPane sp && sp.getStyleClass().contains("tab-header-background"))
                scene.onMousePressed(a);
        });
        tab.setOnMouseDragged(scene::onMouseDragged);

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, x -> {
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
        });

        addTab("pages/main", "        ", false, MainPage.class);

        setBackground(Configurator.getConfig().getBackgroundImage());


        if (Configurator.getConfig().shouldShowHelloDialog()){
            var x = new DStartupConfigurator().execute();
            if (x){
                Configurator.getConfig().setShowHelloDialog(false);
                Configurator.save();
            }
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

    private ScrollPane getScroll(){
        var pane = new ScrollPane();
        pane.setFitToWidth(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setFitToHeight(true);
        pane.addEventFilter(ScrollEvent.SCROLL, a -> {
            double val = a.getDeltaY() * 0.01 * (pane.getHeight() * 1 / 800);
            pane.setVvalue(pane.getVvalue() - val);
        });
        return pane;
    }
    public void closeTab(int index){
        tab.getTabs().remove(index);
    }

    public void relocateTab(int i1, int i2){
        int limit = tab.getTabs().size() - 1;
        if (i2 > limit || i1 > limit)
            return;
        var tabs = new ArrayList<>(tab.getTabs());
        Collections.swap(tabs, i1, i2);
        tab.getTabs().setAll(tabs);
    }

    public <T extends Controller> T replaceTab(Controller dest, String fxml, String title, boolean closable, Class<T> type){
        var t = tab.getTabs().stream().filter(x -> x instanceof CTab ct && dest.equals(ct.getController())).findFirst();
        int index = t.map(tab.getTabs()::indexOf).orElse(-1);
        if (index != -1)
            closeTab(index);

        var t1 = createTab(fxml, title, closable);

        tab.getTabs().add(index, t1);
        tab.getSelectionModel().select(t1);

        handler.execute((KeyEvent) new KeyEvent("tabFocusChange").setSource(t));

        return (T)t1.getController();
    }

    public <T extends Controller> T addTab(String fxml, String title, boolean closable, Class<T> type){
        var t = tab.getTabs().stream().filter(x -> x.getText().equals(title)).findFirst().orElse(null);

        if (t instanceof CTab ct){
            tab.getSelectionModel().select(ct);
            return (T)ct.getController();
        }

        var t1 = createTab(fxml, title, closable);

        tab.getTabs().add(t1);

        handler.execute((KeyEvent) new KeyEvent("tabFocusChange").setSource(tab.getSelectionModel().getSelectedItem()));

        tab.getSelectionModel().select(t1);
        t1.getController().onShown();

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
        t.setOnClosed(a -> t.dispose());
        t.setController(n.setNode(scroll).setParentObject(t).setStage(getStage()));

        return t;
    }

    private void onProgress(ProgressEvent e){
        if (!running.get())
            running.set(true);
        String id = e.getKey().equals("download") ? "mb" : e.getKey();
        setStatus(0, df.format(e.getRemain()) + id + " / " + df.format(e.getTotal()) + id);

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

        this.progress.setProgress(progress);
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
     * Sets the status object.
     * @param priority Priority order of the status. Objects that have different priorities are separated by '|'
     * @param text Status text.
     */
    public void setStatus(int priority, String text){
        if (!running.get())
            return;

        if (priority < 0)
            priority = 0;
        else if (priority >= status.length)
            priority = status.length - 1;

        status[priority] = text;

        var f = Arrays.stream(status).filter(a -> a != null && !a.isBlank()).collect(Collectors.toList());
        Collections.reverse(f);

        String stat = String.join(" | ", f);

        lblStatus.setText(stat);
    }

    /**
     * Sets the status object with the highest priority order.
     * @param text Status text.
     */
    public void setPrimaryStatus(String text){
        for (int i = 1; i < status.length; i++)
            setStatus(i, null);
        setStatus(0, text);
    }

    public EventHandler<KeyEvent> getHandler(){
        return handler;
    }

    public void launch(Profile p, boolean cache, ServerInfo server){
        var wr = (Wrapper<?>)p.getWrapper();
        Vanilla.getVanilla().setDisableCache(cache);
        wr.setDisableCache(cache).getHandler().addHandler("main", this::onGeneralEvent, true);

        var task = new Task<>() {
            @Override
            protected Object call() throws NoConnectionException, StopException, HttpException, FileNotFoundException, PerformException, VersionNotFoundException {
                Launcher.getLauncher().prepare(p);
                Cat.sleep(200);

                if (wr.isStopRequested()){
                    wr.setStopRequested(false);
                    Vanilla.getVanilla().setStopRequested(false);
                    running.set(false);
                    throw new StopException();
                }

                Platform.runLater(() -> clearStatus());

                Launcher.getLauncher().launch(ExecutionInfo.fromProfile(p).includeServer(server));

                Cat.sleep(200);
                if (Configurator.getConfig().hideAfter())
                    UI.getUI().showAll();

                wr.setDisableCache(false);
                Vanilla.getVanilla().setDisableCache(false);

                return null;
            }
        };

        task.setOnFailed(a -> {
            var f = a.getSource().getException();

            Cat.sleep(500);
            Platform.runLater(() -> {
                running.set(false);
                wr.setStopRequested(false);
                Vanilla.getVanilla().setStopRequested(false);
            });

            if (f instanceof NoConnectionException){
                announceLater(Translator.translate("error.oops"),Translator.translate("error.connection"), Announcement.AnnouncementType.ERROR, Duration.millis(3000));
                //Platform.runLater(() -> setPrimaryStatus());
            }
            else if (f instanceof VersionNotFoundException e){
                announceLater(Translator.translate("error.oops"), Translator.translateFormat("error.noVersion", e.getMessage()), Announcement.AnnouncementType.ERROR, Duration.millis(3000));
                //Platform.runLater(() -> setPrimaryStatus());
            }
            else if (f instanceof StopException){
                //
            }
            else if (f instanceof PerformException pe){
                announceLater(Translator.translate("error.oops"), pe.getMessage(), Announcement.AnnouncementType.ERROR, Duration.millis(3000));
                //Platform.runLater(() -> setPrimaryStatus(pe.getMessage()));
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
}
