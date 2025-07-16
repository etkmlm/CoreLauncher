package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.entity.GrabVector;
import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.FDObject;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CMenu;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.ui.dialog.DProfileSelector;
import com.laeben.corelauncher.ui.util.ProfileUtil;
import com.laeben.corelauncher.util.ImageCacheManager;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public class CGroup extends CDockObject {

    public static final String ADD = "add";

    public record TransferInfo(CGroup group, List<CDockObject> cdo, double x, double y){}

    @FXML
    private GridPane folderRoot;

    private final Popup gPopup;
    private final CMenu menuGroup;
    private final CButton menuGroupButton;
    private final DProfileSelector selector;

    private final VBox mainPane;
    private final FlowPane lvProfiles;
    private final ScaleTransition trns;
    private final TextField txtName;
    private final CButton btnRename;

    private final CView miniatureOut;
    private final CView miniatureIn;

    private CProfile on;
    private boolean isOut;
    private long outSince;
    private double lX;
    private double lY;
    private double fX,fY;

    private double width;
    private double height;

    private Bounds scenelocal;
    private HBox topBox;

    private Consumer<TransferInfo> onTransfer;

    private final EventHandler onRootEvent;

    public CGroup(){
        gPopup = new Popup();
        //gPopup.setWidth(200);
        //gPopup.setHeight(100);
        gPopup.setAutoHide(true);
        gPopup.setAutoFix(false);

        menuGroup = new CMenu();
        gPopup.getContent().add(menuGroup);

        menuGroupButton = new CButton();
        menuGroupButton.setVisible(false);
        menuGroupButton.setManaged(false);

        selector = new DProfileSelector(DProfileSelector.Functionality.MULTIPLE_PROFILE_SELECTOR);

        miniatureOut = new CView();
        miniatureOut.setCornerRadius(48, 48, 8);
        miniatureOut.setFitWidth(48);
        miniatureOut.setFitHeight(48);
        miniatureOut.setManaged(false);
        miniatureOut.setVisible(false);
        miniatureOut.setViewOrder(-1);
        root.getChildren().add(miniatureOut);

        miniatureIn = new CView();
        miniatureIn.setCornerRadius(48, 48, 8);
        miniatureIn.setFitWidth(48);
        miniatureIn.setFitHeight(48);
        miniatureIn.setManaged(false);
        miniatureIn.setVisible(false);
        miniatureIn.setViewOrder(-1);
        gPopup.getContent().add(miniatureIn);
        gPopup.setHideOnEscape(false);



        mainPane = new VBox();
        mainPane.setSpacing(10);
        mainPane.setScaleY(0);
        mainPane.setStyle("-fx-padding: 10px; -fx-background-color: -control-fill-secondary; -fx-background-radius: 10px;-fx-min-width: 200px;-fx-min-height: 200px");

        gPopup.getContent().add(mainPane);

        lvProfiles = new FlowPane();
        lvProfiles.setHgap(24);
        lvProfiles.setVgap(40);

        btnRename = new CButton();
        btnRename.setVisible(false);
        btnRename.setStyle("-fx-font-size: 14pt;-fx-pref-height: 45px");
        btnRename.setText(Translator.translate("option.done"));

        txtName = new TextField();
        txtName.setStyle("-fx-background-color: transparent; -fx-font-weight: bolder;");
        txtName.setFocusTraversable(false);
        txtName.focusedProperty().addListener(a -> {
            if (txtName.isFocused()){
                btnRename.setVisible(true);
            }
            else if (!btnRename.isFocused()){
                btnRename.setVisible(false);
            }
        });
        txtName.setMaxWidth(Double.MAX_VALUE);

        trns = new ScaleTransition();
        trns.setNode(mainPane);
        trns.setDuration(Duration.seconds(0.5));
        trns.setFromY(0);
        trns.setToY(1);

        onRootEvent = this::onRootEvent;
    }

    private void selectAllObjects(){
        lvProfiles.getChildren().forEach(a -> ((CDockObject)a).setSelected(true));
    }

    private void deselectAllObjects(){
        lvProfiles.getChildren().forEach(a -> ((CDockObject)a).setSelected(false));
    }

    public void hide(){
        gPopup.hide();
    }

    @Override
    protected void onVanished(ActionEvent e){
        if (object == null)
            return;

        FloatDock.getDock().remove(object);
    }

    public void setOnTransfer(Consumer<TransferInfo> onTransfer){
        this.onTransfer = onTransfer;
    }

    @Override
    protected boolean onSet(FDObject item) {
        object = item;

        menu.clear();
        menu.setButton(new CButton());
        menu.addItem(ImageCacheManager.getImage("delete.png", 48), CDockObject.DELETE, Translator.translate("option.delete"), a -> vanish());
        menu.addItem(ImageCacheManager.getImage("export.png", 48), CDockObject.EXPORT, Translator.translate("profile.menu.export"), a -> ProfileUtil.export(object, menu.getScene().getWindow()));
        menu.addItem(ImageCacheManager.getImage("backup.png", 48), CDockObject.BACKUP, Translator.translate("profile.menu.backup"), a -> ProfileUtil.backup(object, menu.getScene().getWindow()));

        menuGroup.clear();
        menuGroup.setButton(menuGroupButton);
        menuGroup.addItem(null, ADD, Translator.translate("profile.menu.add"), a -> {
            var result = selector.show(List.of(), Profiler.getProfiler().getAllProfiles());
            if (result.isEmpty())
                return;
            FloatDock.getDock().addToGroup(item, result.get().getProfiles());
        });

        txtName.setOnKeyPressed(a -> {
            if (a.getCode() != KeyCode.ENTER)
                return;

            FloatDock.getDock().renameGroup(item, txtName.getText());

            mainPane.requestFocus();
        });

        btnRename.setOnMouseClicked(a -> {
            FloatDock.getDock().renameGroup(item, txtName.getText());

            mainPane.requestFocus();

            btnRename.setVisible(false);
        });

        gPopup.addEventFilter(EventType.ROOT, onRootEvent);

        mainPane.getChildren().clear();

        gPopup.showingProperty().addListener(a -> {
            if (!gPopup.isShowing())
                mainPane.requestFocus();
        });

        topBox = new HBox();
        topBox.setAlignment(Pos.CENTER);
        topBox.setSpacing(8);
        topBox.setPrefHeight(40);
        topBox.setFillHeight(true);
        topBox.getChildren().add(txtName);
        topBox.getChildren().add(btnRename);

        HBox.setHgrow(txtName, Priority.ALWAYS);
        mainPane.getChildren().add(topBox);

        reloadItems();

        lvProfiles.setPrefWidth(520);
        lvProfiles.setMinHeight(100);
        lvProfiles.setPadding(new Insets(16, 16, 32, 16));
        var scroll = new ScrollPane();
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToWidth(true);
        scroll.setContent(lvProfiles);
        scroll.setMaxHeight(364);

        mainPane.getChildren().add(scroll);

        mainPane.getStylesheets().clear();
        mainPane.getStylesheets().add(CoreLauncherFX.CLUI_CSS);

        folderRoot.setVisible(true);
        folderRoot.setManaged(true);


        reloadMeta();

        return true;
    }

    private void onRootEvent(Event e){
        if (e instanceof javafx.scene.input.KeyEvent a){
            if (txtName.isFocused())
                return;

            if (a.isControlDown() && a.getCode() == KeyCode.A){
                selectAllObjects();
                return;
            }

            if (a.getCode() == KeyCode.ESCAPE){
                deselectAllObjects();
            }
            else if (a.getCode() == KeyCode.DELETE || a.getCode() == KeyCode.BACK_SPACE){
                getSelectedItems().forEach(x -> FloatDock.getDock().removeFromGroup(object, x.getPrimaryProfile()));
                reloadItems();
            }
        }
        else if (e instanceof MouseEvent a && a.getEventType() == MouseEvent.MOUSE_CLICKED)
            onClicked(a);
    }

    private void onClicked(MouseEvent a){
        /*var bounds = topBox.localToParent(topBox.getBoundsInLocal());
        if (a.getButton() != MouseButton.SECONDARY || bounds.contains(a.getX(), a.getY()))
            return;*/
        if (a.getButton() != MouseButton.SECONDARY || (!a.getTarget().equals(lvProfiles) && !a.getTarget().getClass().getName().contains("ScrollPaneSkin")))
            return;

        menuGroupButton.setLayoutX(a.getX());
        menuGroupButton.setLayoutY(a.getY());
        menuGroup.show();
    }

    private List<CDockObject> getSelectedItems(){
        return lvProfiles.getChildren().stream().map(a -> (CDockObject)a).filter(x -> x.getPrimaryProfile() != null && x.getSelected()).toList();
    }

    private void onProfileEvent(KeyEvent e){
        if (e.getKey().equals(CDockObject.SELECT)){
            Main.getMain().selectProfile((Profile) e.getSource());
            hide();
        }
        else if (e.getKey().equals(FloatDock.REMOVE)){
            reloadItems();
        }
    }

    private void setMiniatureImage(Image img){
        miniatureIn.setImage(img);
        miniatureOut.setImage(img);
        if (img == null){
            miniatureIn.setVisible(false);
            miniatureOut.setVisible(false);
        }
    }

    private void onProfileDragEvent(BaseEvent e){
        var cdo = (CDockObject)e.getSource();
        if (!(e instanceof KeyEvent ke) || cdo.getPrimaryProfile() == null)
            return;
        if (ke.getKey().equals(CDockObject.RELEASE)){
            setMiniatureImage(null);

            if (isOut && System.currentTimeMillis() > outSince + 1000){
                isOut = false;
                on = null;
                outSince = 0;

                if (onTransfer != null){
                    var selected = getSelectedItems();

                    hide();
                    onTransfer.accept(new TransferInfo(this, selected.isEmpty() ? List.of(cdo) : selected, scenelocal.getMinX() + miniatureOut.getLayoutX(), scenelocal.getMinY() + miniatureOut.getLayoutY()));
                }

                return;
            }

            getSelectedItems().forEach(a -> a.setVisible(true));
            cdo.setVisible(true);

            if (on == null)
                return;

            int index = lvProfiles.getChildren().indexOf(on);
            lvProfiles.getChildren().remove(cdo);
            lvProfiles.getChildren().add(index, cdo);

            FloatDock.getDock().reorderGroup(object, cdo.getPrimaryProfile(), index);
        }
        else if (ke.getKey().equals(CDockObject.MOVE) && ke instanceof ValueEvent ve){
            var vec = (GrabVector)ve.getValue();
            if (miniatureIn.getImage() == null){
                setMiniatureImage(ImageCacheManager.getImage(cdo.getPrimaryProfile(), 48));
                var items = getSelectedItems();
                items.forEach(a -> a.setVisible(false));
                if (!items.isEmpty() && !items.contains(cdo))
                    cdo.setSelected(true);
                cdo.setVisible(false);

                width = gPopup.getWidth();
                height = gPopup.getHeight();
            }

            var point = new Point2D(vec.mouseX(), vec.mouseY());
            boolean x1 = (point.getX() >= 0) && (point.getY() >= 0);
            boolean x2 = point.getX() <= width;
            boolean x3 = point.getY() <= height;
            if (x1 && x2 && x3){
                isOut = false;
                outSince = 0;
                for (var i : lvProfiles.getChildren()){
                    var c = (CProfile)i;
                    var bounds = c.localToParent(c.getBoundsInLocal());
                    if (bounds.contains(point)){
                        if (!c.getObject().equals(cdo.getObject()))
                            on = c;
                        break;
                    }
                    else
                        on = null;
                }

                miniatureIn.setVisible(true);
                miniatureOut.setVisible(false);
                miniatureIn.setLayoutX(vec.mouseX());
                miniatureIn.setLayoutY(vec.mouseY());
            }
            else{
                isOut = true;
                on = null;
                if (outSince == 0)
                    outSince = System.currentTimeMillis();

                miniatureIn.setVisible(false);
                miniatureOut.setVisible(true);
                miniatureOut.setLayoutX(vec.mouseX() + lX);
                miniatureOut.setLayoutY(vec.mouseY() + lY);
            }
        }
    }

    @Override
    protected void onMouseReleased(MouseEvent e){
        if (moving)
            return;
        show(e.getScreenX(), e.getScreenY());
    }

    public void show(double x, double y){
        this.fX = x;
        this.fY = y;
        trns.playFromStart();
        gPopup.show(folderRoot.getScene().getWindow(), x, y);
        var pos = root.localToScreen(root.getBoundsInLocal());
        scenelocal = root.localToScene(root.getBoundsInLocal());
        lX = x - pos.getMinX();
        lY = y - pos.getMinY();
    }

    public void close(){
        gPopup.hide();
    }

    @Override
    protected void drag() {

    }

    public void reloadItems(){
        lvProfiles.getChildren().clear();
        for (int i = 0; i < object.getProfiles().size(); i++)
            lvProfiles.getChildren().add(new CProfile()
                    .setOnMenuClick(a -> {
                        if (a.equals(CDockObject.EDIT) || a.equals(CDockObject.PAGE) || a.equals(CDockObject.COPY))
                            close();
                        return true;
                    })
                    .set(FDObject.createSingle(object.getProfiles().get(i), i, 0))
                    .setListener(this::onProfileEvent)
                    .setGrabListener(this::onProfileDragEvent)
                    .setParent(object)
            );
    }

    public void reloadMeta(){
        lblName.setText(object.getName());
        txtName.setText(object.getName());
        var profiles = object.getProfiles();

        folderRoot.getChildren().clear();

        // 4 Thumbnails
        for(int i = 0; i < profiles.size() && i < 4; i++){
            var box = new HBox();
            var p = profiles.get(i);
            box.setBackground(new Background(new BackgroundImage(ImageCacheManager.getImage(p, 48), null, null, null, null)));

            GridPane.setColumnIndex(box, i % 2);
            GridPane.setRowIndex(box, i == 0 ? 0 : (i - i%2) / 2);
            folderRoot.getChildren().add(box);

            box.widthProperty().addListener(a -> {
                var rect = new Rectangle(48, 48);
                rect.setArcHeight(16);
                rect.setArcWidth(16);
                box.setClip(rect);
            });
        }
    }

    @Override
    public void dispose(){
        gPopup.removeEventFilter(EventType.ROOT, onRootEvent);
    }
}
