package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.ui.entity.GrabVector;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.FDObject;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.page.EditProfilePage;
import com.laeben.corelauncher.ui.controller.page.ProfilePage;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CMenu;
import com.laeben.corelauncher.ui.entity.EventFilter;
import com.laeben.corelauncher.ui.util.EventFilterManager;
import com.laeben.corelauncher.ui.util.ProfileUtil;
import com.laeben.corelauncher.util.ImageCacheManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.controlsfx.control.GridCell;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class CDockObject extends GridCell {
    public static final String RELEASE = "release";
    public static final String MOVE = "move";
    public static final String EDIT = "edit";
    public static final String PAGE = "detail";
    public static final String DELETE = "delete";
    public static final String BACKUP = "backup";
    public static final String EXPORT = "export";
    public static final String OPEN = "open";
    public static final String COPY = "copy";
    public static final String SELECT = "select";
    public static final String SHORTCUT = "shortcut";

    public static final DataFormat dataFormat = new DataFormat("profile");
    public static final Duration animationDuration = Duration.seconds(0.8);

    private final Node gr;
    protected FDObject object;
    protected FDObject parent;
    private final ScaleTransition disappear;
    private final FadeTransition hoverOpacity;

    protected Consumer<KeyEvent> listener;
    private Consumer<BaseEvent> grabListener;

    private final BooleanProperty selected;

    private boolean listenerRegistered = false;
    private final ChangeListener<Boolean> onFocusChangeValue;

    private final EventFilterManager efManager;

    public CDockObject(){
        setGraphic(gr = UI.getUI().load(CoreLauncherFX.class.getResource("layout/cells/cdobject.fxml"), this));

        efManager = new EventFilterManager();

        hoverOpacity = new FadeTransition();
        hoverOpacity.setFromValue(1);
        hoverOpacity.setToValue(0.8);
        hoverOpacity.setDuration(animationDuration);

        disappear = new ScaleTransition();
        disappear.setFromX(1);
        disappear.setToX(0);
        disappear.setDuration(Duration.millis(300));
        disappear.setOnFinished(this::onVanished);

        selected = new SimpleBooleanProperty();

        selected.addListener((a, os, ns) -> {
            if (ns != null && ns)
                setOpacity(0.8);
            else
                setOpacity(1);
        });

        onFocusChangeValue = this::onFocusChange;

        sceneProperty().addListener((a, b, scene) -> {
            if (listenerRegistered)
                return;

            efManager.addEventFilter(EventFilter.node(root, MouseEvent.DRAG_DETECTED, e -> drag()));
            efManager.addEventFilter(EventFilter.node(root, MouseEvent.MOUSE_DRAGGED, this::onDragged));
            efManager.addEventFilter(EventFilter.node(root, MouseEvent.MOUSE_PRESSED, this::onPressed));
            efManager.addEventFilter(EventFilter.node(root, MouseEvent.MOUSE_RELEASED, this::onReleased));

            scene.getWindow().focusedProperty().addListener(onFocusChangeValue);
            listenerRegistered = true;
        });
    }

    @FXML
    protected VBox root;
    @FXML
    protected CMenu menu;
    @FXML
    protected Label lblName;
    @FXML
    protected CButton btnSelect;

    protected boolean moving;
    protected boolean exporting;
    protected boolean pressed;
    protected long pressTime;
    protected double pressRelativeMouseX;
    protected double pressRelativeMouseY;
    protected double pressLayoutX;
    protected double pressLayoutY;

    public static CDockObject get(FDObject item){
        return item.isSingle() ? new CProfile().set(item) : new CGroup().set(item);
    }

    protected abstract boolean onSet(FDObject item);
    protected abstract void drag();
    protected void onMouseExited(MouseEvent e){}
    protected void onMouseEntered(MouseEvent e){}
    protected void onMouseReleased(MouseEvent e){}

    public CDockObject set(FDObject item){
        if (item == null || (item.type == FDObject.FDType.SINGLE && item.getProfiles().isEmpty())){
            setGraphic(null);
            return this;
        }

        this.object = item;

        hoverOpacity.setNode(root);
        disappear.setNode(root);

        if (!onSet(item))
            return null;

        lblName.setText(item.getName());
        lblName.setTooltip(new Tooltip(item.getName()));
        //lblName.setManaged(false);

        root.setOnMouseEntered(a -> {
            hoverOpacity.playFromStart();
            onMouseEntered(a);
        });
        root.setOnMouseExited(a -> {
            hoverOpacity.playFromStart();
            hoverOpacity.jumpTo(Duration.ZERO);
            hoverOpacity.stop();

            onMouseExited(a);
        });

        setGraphic(gr);

        return this;
    }

    private void onFocusChange(Observable o, Boolean v1, Boolean v2) {
        if ((v2 == null || !v2) || (!moving && !exporting))
            return;

        onReleasedIn();
    }

    private void onReleased(MouseEvent a){
        if (a.getButton() == MouseButton.SECONDARY){
            menu.show();
            return;
        }
        if (a.isControlDown()){
            setSelected(!getSelected());
            return;
        }

        onMouseReleased(a);
        onReleasedIn();
    }

    private void onReleasedIn(){
        root.setScaleX(1);
        root.setScaleY(1);

        grabListener.accept(new KeyEvent(RELEASE).setSource(this));
        moving = false;
        exporting = false;
    }

    private void onPressed(MouseEvent a){
        pressed = true;
        pressTime = System.currentTimeMillis();
        pressRelativeMouseX = a.getX();
        pressRelativeMouseY = a.getY();
        pressLayoutX = getLayoutX();
        pressLayoutY = getLayoutY();
    }
    private void onDragged(MouseEvent a){
        a.setDragDetect(false);
        if (!pressed){
            if (exporting)
                return;

            long millis = System.currentTimeMillis();

            var wnd = getScene().getWindow();

            if (wnd instanceof Popup){
                exporting = false;
            }
            else{
                double x = wnd.getX();
                double y = wnd.getY();
                double w = wnd.getWidth();
                double h = wnd.getHeight();

                double mX = a.getScreenX();
                double mY = a.getScreenY();

                exporting = (mX < x || mX > x + w) || (mY < y || mY > y + h);
            }



            if (exporting){
                moving = false;
                onReleased(a);
                a.setDragDetect(true);
            }
            else{
                grabListener.accept(new ValueEvent(MOVE, new GrabVector(a.getSceneX(), a.getSceneY(), pressRelativeMouseX, pressRelativeMouseY)).setSource(this));
            }
            return;
        }

        pressed = false;

        root.setScaleX(1.1);
        root.setScaleY(1.1);

        moving = true;

        onDragged(a);
    }

    public boolean isMoving(){
        return moving;
    }

    public boolean isExporting(){
        return exporting;
    }

    public double getPressRelativeMouseX() {
        return pressRelativeMouseX;
    }

    public double getPressRelativeMouseY() {
        return pressRelativeMouseY;
    }

    public double getPressLayoutX() {
        return pressLayoutX;
    }

    public double getPressLayoutY() {
        return pressLayoutY;
    }

    public static void generateProfileMenu(CMenu menu, Profile profile, CButton menuButton, Predicate<String> onAction){
        menu.setButton(menuButton);

        menu.addItem(ImageCacheManager.getImage("pages.png", 32), PAGE, Translator.translate("profile.menu.page"), a -> {
            if (onAction != null && !onAction.test(a.key()))
                return;

            Main.getMain().addTab("pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile);
        });
        menu.addItem(ImageCacheManager.getImage("edit.png", 32), EDIT, Translator.translate("option.edit"), a -> {
            if (onAction != null && !onAction.test(a.key()))
                return;

            Main.getMain().addTab("pages/profileedit", "", true, EditProfilePage.class).setProfile(profile);
        });
        menu.addItem(ImageCacheManager.getImage("export.png", 32), EXPORT, Translator.translate("profile.menu.export"), a -> {
            if (onAction != null && !onAction.test(a.key()))
                return;
            ProfileUtil.export(profile, menuButton.getScene().getWindow());
        });
        /*menu.addItem(i, Translator.translate("option.delete"), a -> {
            var d = FloatDock.getDock().findObject(profile);
            d.ifPresent(FloatDock.getDock()::remove);
            Profiler.getProfiler().deleteProfile(profile);
        });*/
        menu.addItem(ImageCacheManager.getImage("delete.png", 32), DELETE, Translator.translate("option.delete"), a -> {
            if (onAction != null && !onAction.test(a.key()))
                return;
            Profiler.getProfiler().deleteProfile(profile);
        });
        menu.addItem(ImageCacheManager.getImage("folder.png", 32), OPEN, Translator.translate("profile.menu.open"), a -> {
            if (onAction != null && !onAction.test(a.key()))
                return;
            OSUtil.open(profile.getPath().toFile());
        });
        menu.addItem(ImageCacheManager.getImage("backup.png", 48), BACKUP, Translator.translate("profile.menu.backup"), a -> {
            if (onAction != null && !onAction.test(a.key()))
                return;
            ProfileUtil.backup(profile, menuButton.getScene().getWindow());
        });
        menu.addItem(ImageCacheManager.getImage("copy.png", 32), COPY, Translator.translate("profile.menu.copy"), a -> {
            if (onAction != null && !onAction.test(a.key()))
                return;
            var p = Profiler.getProfiler().copyProfile(profile);
            Main.getMain().addTab("pages/profile", p.getName(), true, ProfilePage.class).setProfile(p);
        });
        if (CoreLauncher.SYSTEM_OS == OS.WINDOWS || CoreLauncher.SYSTEM_OS == OS.LINUX)
            menu.addItem(ImageCacheManager.getImage("shortcut.png", 32), SHORTCUT, Translator.translate("profile.menu.shortcut"), a -> {
                if (onAction != null && !onAction.test(a.key()))
                    return;
                ProfileUtil.createShortcut(profile, menuButton.getScene().getWindow());
            });
    }

    public FDObject getObject(){
        return object;
    }
    public Profile getPrimaryProfile(){
        return object.getProfiles().stream().findFirst().orElse(Profile.empty());
    }

    public void selectPrimary(){
        if (moving || exporting || listener == null)
            return;
        listener.accept((KeyEvent) new KeyEvent(SELECT).setSource(getPrimaryProfile()));
    }

    public CDockObject setListener(Consumer<KeyEvent> listener){
        this.listener = listener;
        return this;
    }
    public CDockObject setGrabListener(Consumer<BaseEvent> listener){
        this.grabListener = listener;
        return this;
    }
    public CDockObject setParent(FDObject parent){
        this.parent = parent;
        return this;
    }

    public void vanish(){
        disappear.playFromStart();
    }

    public BooleanProperty getSelectedProperty(){
        return selected;
    }

    public void setSelected(boolean s){
        selected.set(s);
    }

    public boolean getSelected(){
        return selected.get();
    }

    protected void onVanished(ActionEvent e){}

    public void dispose(){
        listener = null;
        grabListener = null;
        getScene().getWindow().focusedProperty().removeListener(onFocusChangeValue);
        efManager.clear();
    }
}