package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.Path;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.ui.entity.GrabVector;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.FDObject;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.cell.CGroup;
import com.laeben.corelauncher.ui.controller.cell.CDockObject;
import com.laeben.corelauncher.ui.controller.cell.CProfile;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.dialog.DProfileSelector;
import com.laeben.corelauncher.ui.util.ProfileUtil;
import com.laeben.corelauncher.util.EventHandler;
import javafx.animation.ScaleTransition;
import com.laeben.corelauncher.api.ui.UI;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.input.*;
import javafx.util.Duration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MainPage extends HandlerController {
    public static final String KEY = "pgmain";

    @FXML
    public SelectionPane<CDockObject> root;

    private boolean agreedMove = false;

    private final CMenu dockContext;
    private final CButton dockGhost;

    private final DProfileSelector selector;

    private final ScaleTransition showUpTransition;

    private double lX;
    private double lY;

    private boolean transferMode = false;

    public MainPage(){
        super(KEY);
        showUpTransition = new ScaleTransition();
        showUpTransition.setDuration(Duration.millis(300));
        showUpTransition.setFromX(0);
        showUpTransition.setToX(1);

        selector = new DProfileSelector(DProfileSelector.Functionality.DOCK_SELECTOR);

        dockContext = new CMenu();
        dockContext.addItem(null, Translator.translate("dock.menu.profiles"), a -> {
            var f = root.localToScreen(root.getBoundsInLocal());
            lX = a.getScreenX() - f.getMinX();
            lY = a.getScreenY() - f.getMinY();

            var result = selector.show(
                    root.getChildren().stream().filter(x -> x instanceof CGroup).map(x -> ((CGroup)x).getObject().getName()).toList(),
                    Profiler.getProfiler().getAllProfiles());

            if (result.isEmpty())
                return;

            if (result.get() instanceof DProfileSelector.GroupResult gres){
                if (gres.createGroup()){
                    FloatDock.getDock().place(FDObject.createGroup(gres.getProfiles(), lX, lY, FloatDock.getDock().generateName(Translator.translate("dock.menu.group.new"))), false);
                }
                else {
                    var group = FloatDock.getDock().findObject(gres.getGroupName());
                    group.ifPresent(gr -> FloatDock.getDock().addToGroup(gr, gres.getProfiles()));
                }
            }
            else{
                for (var p : result.get().getProfiles()){
                    FloatDock.getDock().place(FDObject.createSingle(p, lX, lY), false);
                }
            }
        });
        dockContext.addItem(null, Translator.translate("dock.menu.group.new"), a -> {
            var f = root.localToScreen(root.getBoundsInLocal());
            FloatDock.getDock().place(FDObject.createGroup(List.of(), a.getScreenX() - f.getMinX(), a.getScreenY() - f.getMinY(), FloatDock.getDock().generateName(Translator.translate("dock.menu.group.new"))), false);
        });
        dockContext.addItem(null, Translator.translate("dock.menu.profile"), a -> {
            var f = root.localToScreen(root.getBoundsInLocal());
            lX = a.getScreenX() - f.getMinX();
            lY = a.getScreenY() - f.getMinY();

            Main.getMain().addTab("pages/profileedit", Translator.translate("frame.title.pedit"), true, EditProfilePage.class);
        });
        dockContext.addItem(null, Translator.translate("dock.menu.browser"), a -> {
            var f = root.localToScreen(root.getBoundsInLocal());
            lX = a.getScreenX() - f.getMinX();
            lY = a.getScreenY() - f.getMinY();

            Main.getMain().addTab("pages/browser", Translator.translate("frame.title.browser"), true, BrowserPage.class).setProfile(null);
        });

        dockGhost = new CButton();
        dockGhost.setScaleX(0);
        dockGhost.setScaleY(0);

        dockContext.setButton(dockGhost);

        registerHandler(FloatDock.getDock().getHandler(), a -> {
            var obj = (FDObject)a.getSource();
            if (a instanceof KeyEvent e){
                if (e.getKey().equals(FloatDock.PLACE)){
                    var c = placeObj(obj);
                    if (!transferMode){
                        showUpTransition.setNode(c);
                        showUpTransition.playFromStart();
                    }
                }
                else if (e.getKey().equals(FloatDock.REPLACE)){
                    removeObj(obj);
                    var c = placeObj(obj);
                    showUpTransition.setNode(c);
                    showUpTransition.playFromStart();
                }
                else if (e.getKey().equals(FloatDock.REMOVE)){
                    root.getChildren().stream()
                            .filter(x -> (x instanceof CDockObject cd) && cd.getObject().equals(obj) && cd.getSelected())
                            .findFirst().ifPresent(x -> ((CDockObject)x).setSelected(false));
                    removeObj(obj);
                }
                else if (e.getKey().equals(FloatDock.UPDATE)){
                    if (obj.type == FDObject.FDType.GROUP)
                        root.getChildren().stream().filter(x -> x instanceof CGroup cg && cg.getObject().equals(obj)).findFirst().ifPresent(n -> {
                            var f = (CGroup)n;
                            f.reloadItems();
                            f.reloadMeta();
                        });
                    else {
                        removeObj(obj);
                        placeObj(obj);
                    }
                }
                else if (e.getKey().equals(EventHandler.RELOAD)){
                    root.clearSelection();
                    root.getChildren().removeIf(x -> x instanceof CDockObject);
                    reloadDock();
                }
            }
        }, true);
        registerHandler(Profiler.getProfiler().getHandler(), a -> {
            if (!a.getKey().equals(Profiler.PROFILE_CREATE) || !Configurator.getConfig().shouldPlaceNewProfileToDock())
                return;
            var p = ((List<Profile>)a.getNewValue()).get(0);

            FloatDock.getDock().place(FDObject.createSingle(p, lX, lY), false);
        }, true);
        registerHandler(Main.getMain().getHandler(), a -> {
            if (a.getKey().equals(Main.TAB_KEY_PRESS)){
                onKeyPressed((javafx.scene.input.KeyEvent) a.getSource());
                return;
            }
            if (!a.getKey().equals(Main.TAB_FOCUS_CHANGE))
                return;
            if (a.getSource() instanceof CTab ct && ct.getController().equals(this)){
                root.getChildren().stream().filter(x -> x instanceof CGroup).forEach(x -> ((CGroup) x).hide());
                deselectAllObjects();
            }
        }, true);
    }

    private CDockObject getCDObject(FDObject obj){
        var pr = CDockObject.get(obj);
        if (pr == null)
            return null;
        pr.setListener(this::onProfileSelectEvent).setGrabListener(a -> {
            if (!(a instanceof KeyEvent ke))
                return;
            if (ke.getKey().equals(CDockObject.RELEASE)){
                obj.layoutX = pr.getLayoutX();
                obj.layoutY = pr.getLayoutY();

                if (pr instanceof CProfile c && c.getOnObject() != null){
                    var oobj = c.getOnObject();
                    if (oobj instanceof CGroup cg){
                        FloatDock.getDock().moveToGroup(cg.getObject(), agreedMove ? root.getSelectedItems().stream().map(CDockObject::getObject).toList() : List.of(c.getObject()));
                    }
                    else if (oobj instanceof CProfile cp){
                        List<Profile> ps;
                        if (agreedMove){
                            ps = root.getSelectedItems().stream().map(CDockObject::getPrimaryProfile).collect(Collectors.toList());
                            ps.add(cp.getPrimaryProfile());
                            root.getSelectedItems().forEach(x -> FloatDock.getDock().remove(x.getObject()));
                        }
                        else{
                            ps = List.of(c.getPrimaryProfile(), cp.getPrimaryProfile());
                            FloatDock.getDock().remove(c.getObject());
                        }
                        FloatDock.getDock().place(FDObject.createGroup(ps, cp.getLayoutX(), cp.getLayoutY(), FloatDock.getDock().generateName(Translator.translate("dock.menu.group.new"))), false);
                        FloatDock.getDock().remove(cp.getObject());
                    }
                }
                else{
                    FloatDock.getDock().place(obj, true);
                    if (agreedMove)
                        root.getSelectedItems().forEach(x -> x.setVisible(true));
                }

                root.cancelSelection();
                agreedMove = false;
            }
            else if (ke.getKey().equals(CDockObject.MOVE) && ke instanceof ValueEvent ve){
                var pos = root.localToScene(root.getBoundsInLocal());
                var vec = (GrabVector)ve.getValue();

                pr.setLayoutX(vec.mouseX() - pos.getMinX() - vec.padX());
                pr.setLayoutY(vec.mouseY() - pos.getMinY() - vec.padY());

                var point = new Point2D(vec.mouseX(), vec.mouseY());

                if (!(pr instanceof CProfile cr))
                    return;

                agreedMove = cr.getSelected() && root.getSelectedItems().stream().allMatch(x -> x instanceof CProfile);
                if (agreedMove){
                    for (var s : root.getSelectedItems()){
                        if (s != cr)
                            s.setVisible(false);
                    }
                }

                for(var b : root.getChildren()){
                    if (!(b instanceof CDockObject v))
                        continue;
                    if (b == cr)
                        continue;
                    var bounds = v.localToParent(v.getBoundsInLocal());

                    if (bounds.contains(point)){
                        cr.setScaleX(0.5);
                        cr.setScaleY(0.5);
                        cr.setViewOrder(-1);
                        cr.setOnObject(v);
                        break;
                    }
                    else{
                        cr.setScaleX(1);
                        cr.setScaleY(1);
                        cr.setViewOrder(0);
                        cr.setOnObject(null);
                    }
                }
            }
        });
        if (pr instanceof CGroup cg)
            cg.setOnTransfer(a -> {
                for (var cdo : a.cdo()){
                    FloatDock.getDock().removeFromGroup(a.group().getObject(), cdo.getPrimaryProfile());
                    var oj = cdo.getObject();
                    oj.layoutX = a.x();
                    oj.layoutY = a.y();
                    FloatDock.getDock().place(oj, false);
                }
                transferMode = true;
            });
        pr.getSelectedProperty().addListener(a -> {
            if (pr.getSelected())
                root.getSelectedItems().add(pr);
            else
                root.getSelectedItems().remove(pr);
        });
        return pr;
    }

    private void removeObj(FDObject obj){
        var all = root.getChildren().stream().filter(b -> b instanceof CDockObject c && c.getObject().equals(obj)).toList();
        for (var a : all){
            ((CDockObject)a).dispose();
            root.getChildren().remove(a);
        }
        //root.getChildren().removeIf(b -> b instanceof CDockObject c && c.getObject().equals(obj));
    }
    private CDockObject placeObj(FDObject obj){
        CDockObject p;
        if (obj.type == FDObject.FDType.SINGLE && obj.getProfiles().isEmpty())
            return null;
        if (obj.type == FDObject.FDType.SINGLE || obj.type == FDObject.FDType.GROUP)
            p = getCDObject(obj);
        else {
            return null;
        }

        if (p == null)
            return null;

        p.setLayoutX(obj.layoutX);
        p.setLayoutY(obj.layoutY);

        root.getChildren().add(p);

        return p;
    }

    @Override
    public void preInit() {
        root.getChildren().add(dockGhost);

        root.setOnMouseClicked(a -> {
            if (a.getButton() != MouseButton.SECONDARY || !a.getTarget().equals(root))
                return;
            dockGhost.setLayoutX(a.getX());
            dockGhost.setLayoutY(a.getY());
            dockContext.show();
        });

        reloadDock();

        root.setOnDragOver(a -> {
            if (a.getDragboard().getContent(DataFormat.FILES) == null)
                return;

            a.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        });

        root.setOnDragDropped(a -> {
            Object content;
            Object tC;
            if ((content = a.getDragboard().getContent(DataFormat.FILES)) == null || ((tC = a.getDragboard().getContent(DataFormat.PLAIN_TEXT)) != null && tC.equals("inner")))
                return;

            var list = (List<File>) content;

            new Thread(() -> {
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.import.started"), Translator.translateFormat("announce.info.import.importing", list.size()), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
                int c = 0;
                for (var f : list){
                    var path = Path.begin(f.toPath());

                    try{
                        ProfileUtil.importO(path, a.getSceneX(), a.getSceneY());
                        c++;
                    }
                    catch (Exception e){
                        Logger.getLogger().log(e);
                    }
                }
                int fC = c;
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.import.completed"), Translator.translateFormat("announce.info.import.imported", fC), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
            }).start();
        });


        root.getNav().addItem(
                "\uD83D\uDDD1 " + Translator.translate("option.remove"),
                a -> removeSelectedObjects(),
                0
        );

        root.getNav().addItem(
                "\uD83D\uDDD1 " + Translator.translate("option.delete"),
                a -> deleteSelectedObjects(),
                0
        );

        root.setOnSelectBegin(a -> deselectAllObjects());
        root.setOnSelectCancelled(this::deselectAllObjects);

        root.setOnSelected(bounds -> {
            for (var i : root.getChildren()){
                if (!(i instanceof CDockObject obj))
                    continue;
                var bnds = obj.localToParent(obj.getBoundsInLocal());

                if (bounds.contains(bnds))
                    obj.setSelected(true);
            }
        });
    }

    private void reloadDock(){
        var invalid = new ArrayList<FDObject>();
        for (var obj : FloatDock.getDock().getObjects()){
            if (placeObj(obj) == null)
                invalid.add(obj);
        }

        for (var i : invalid)
            FloatDock.getDock().remove(i);
    }

    private void removeSelectedObjects(){
        root.getSelectedItems().forEach(a -> FloatDock.getDock().remove(a.getObject()));
    }

    private void deleteSelectedObjects(){
        root.getSelectedItems().forEach(a -> {
            if (a instanceof CGroup)
                FloatDock.getDock().remove(a.getObject());
            else if (a instanceof CProfile)
                Profiler.getProfiler().deleteProfile(a.getPrimaryProfile());
        });
    }

    private void onKeyPressed(javafx.scene.input.KeyEvent a){
        if (!Main.getMain().getTab().getSelectionModel().getSelectedItem().equals(this.parentObj) || CoreLauncherFX.isAnyPopupOpen())
            return;

        if (a.isControlDown() && a.getCode() == KeyCode.A){
            selectAllObjects();
            return;
        }

        if (!root.isSelectionMenuOpen())
            return;

        if (a.getCode() == KeyCode.ESCAPE){
            deselectAllObjects();
        }
        else if (a.getCode() == KeyCode.DELETE){
            deleteSelectedObjects();
        }
        else if (a.getCode() == KeyCode.BACK_SPACE){
            removeSelectedObjects();
        }
    }

    private void selectAllObjects(){
        root.getChildren().forEach(n -> {
            if (n instanceof CDockObject c)
                c.setSelected(true);
        });
    }

    private void deselectAllObjects(){
        root.getChildren().forEach(n -> {
            if (n instanceof CDockObject c)
                c.setSelected(false);
        });
    }
    public void onProfileSelectEvent(KeyEvent e) {
        if (!e.getKey().equals(CDockObject.SELECT))
            return;

        Main.getMain().selectProfile((Profile) e.getSource());

        if (Configurator.getConfig().isEnabledSelectAndPlayDock()){
            boolean v1 = Main.getMain().launchClick(false);
            if (!v1)
                Main.getMain().launchClick(false);
        }
    }

    @Override
    public void dispose(){
        for (var c : root.getChildren()){
            if (c instanceof CDockObject cdo)
                cdo.dispose();
        }
        super.dispose();
    }

}