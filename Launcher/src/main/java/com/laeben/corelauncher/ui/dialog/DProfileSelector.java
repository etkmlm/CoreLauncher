package com.laeben.corelauncher.ui.dialog;

import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.ui.controller.cell.CDockObject;
import com.laeben.corelauncher.ui.controller.cell.CPLCell;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.util.ProfileUtil;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.controlsfx.control.SearchableComboBox;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DProfileSelector extends CDialog<DProfileSelector.Result> {

    public static class GroupResult extends Result{

        private final String groupName;

        public GroupResult(List<Profile> profiles, String groupName) {
            super(profiles);

            this.groupName = groupName;
        }

        public boolean createGroup(){
            return groupName == null;
        }

        public String getGroupName() {
            return groupName;
        }
    }

    public static class Result{
        private final List<Profile> profiles;

        public Result(List<Profile> profiles){
            this.profiles = profiles;
        }

        public List<Profile> getProfiles(){
            return profiles;
        }

    }

    public enum Functionality{
        SINGLE_PROFILE_SELECTOR, MULTIPLE_PROFILE_SELECTOR, DOCK_SELECTOR
    }

    @FXML
    private VBox pBox;
    @FXML
    private CField txtSearch;
    @FXML
    private Pane pa;
    @FXML
    private SearchableComboBox<String> cGroups;
    @FXML
    private CList<Profile> pList;
    @FXML
    private CButton btnClose;
    @FXML
    private ScrollPane scroll;

    private List<String> groups;
    private List<Profile> profiles;

    private final Functionality fn;


    public DProfileSelector(Functionality f){
        super("layout/dialog/profileselector.fxml", true);

        fn = f;

        pa.visibleProperty().addListener(a -> {
            if (pa.isVisible())
                cGroups.show();
        });
        if (f == Functionality.DOCK_SELECTOR){
            cGroups.showingProperty().addListener(a -> {
                if (!cGroups.isShowing())
                    pa.setVisible(false);
            });
            cGroups.getSelectionModel().selectedIndexProperty().addListener(a -> {
                int ix = cGroups.getSelectionModel().getSelectedIndex();
                if (ix == -1 || pList.getSelectedItems().isEmpty())
                    return;

                if (ix == 0){
                    close(new GroupResult(pList.getSelectedItems().stream().toList(), null));
                }
                else{
                    close(new GroupResult(pList.getSelectedItems().stream().toList(), cGroups.getItems().get(ix)));
                }
            });

            pList.getNav().addItem("☐ " + Translator.translate("dock.menu.group"), a -> {
                cGroups.setLayoutX(a.getSceneX());
                cGroups.setLayoutY(a.getSceneY());
                cGroups.setValue(null);
                pa.setVisible(true);
            }, 1);
        }
        else if (f == Functionality.SINGLE_PROFILE_SELECTOR){
            pList.setSelectionEnabled(false);
        }
        else if (f == Functionality.MULTIPLE_PROFILE_SELECTOR){
            //pList.setSelectionMode(true);
        }

        pList.getNav().addItem("⤤ " + Translator.translate("profile.menu.export"), a -> exportSelectedProfiles(), 1);
        pList.getNav().addItem("\uD83D\uDDD1 " + Translator.translate("option.delete"),a -> deleteSelectedProfiles(), 1);
        pList.getNav().addItem("✓ " + Translator.translate("option.done"), a -> close(new Result(pList.getSelectedItems())), 1);

        pList.setFilterFactory(a -> a.input().getName().toLowerCase(Locale.getDefault()).contains(a.query().toLowerCase(Locale.getDefault())));
        pList.setCellFactory(() -> new CPLCell()
                .setSelectionDisabled(f == Functionality.SINGLE_PROFILE_SELECTOR)
                .includePlayButton(f == Functionality.DOCK_SELECTOR)
                .setOnClick(a -> {
                    var cell = (CPLCell)a.getSource();
                    switch (a.getKey()){
                        case CPLCell.EXECUTOR_ROOT:
                            if (f == Functionality.DOCK_SELECTOR || f == Functionality.SINGLE_PROFILE_SELECTOR)
                                close(new Result(List.of(cell.getItem())));
                            break;
                        case CPLCell.EXECUTOR_MENU:
                            if (a.getValue().equals(CDockObject.DELETE)){
                                if (cell.isSelected()){
                                    deleteSelectedProfiles();
                                    return false;
                                }
                                else
                                    pList.getItems().remove(cell.getItem());
                            }
                            else if (a.getValue().equals(CDockObject.EXPORT)){
                                if (cell.isSelected()){
                                    exportSelectedProfiles();
                                    return false;
                                }
                            }
                            else if (a.getValue().equals(CDockObject.BACKUP)){
                                if (cell.isSelected()){
                                    backupSelectedProfiles();
                                    return false;
                                }
                            }
                            else if (
                                    a.getValue().equals(CDockObject.EDIT) ||
                                    a.getValue().equals(CDockObject.COPY) ||
                                    a.getValue().equals(CDockObject.PAGE)
                            )
                                close(null);
                            break;
                        default:
                            close(null);

                    }

                    return true;
                })
        );

        pList.setLoadLimit(5);
        scroll.vvalueProperty().addListener((a, b, c) -> {
            if (b.equals(c) || scroll.getVmax() > c.doubleValue())
                return;
            pList.load(false);
        });

        txtSearch.setFocusedAnimation(Duration.millis(200));
        txtSearch.textProperty().addListener(a -> pList.filter(txtSearch.getText()));

        getDialogPane().getScene().getWindow().addEventFilter(EventType.ROOT, x -> {
            if (x instanceof javafx.scene.input.KeyEvent e){
                if (e.getTarget().equals(txtSearch))
                    return;

                if (pList.onKeyEvent(e))
                    return;

                if (e.getCode() == KeyCode.DELETE)
                    deleteSelectedProfiles();
            }
        });

        btnClose.enableTransparentAnimation();

        btnClose.setOnMouseClicked(a -> close(null));

        txtSearch.setOnKeyPressed(a -> {
            if (a.getCode() == KeyCode.ESCAPE)
                node.requestFocus();
        });
    }

    private void deleteSelectedProfiles(){
        var h = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO).executeForResult();
        if (!(h.isPresent() && h.get().result() == CMsgBox.ResultType.YES))
            return;

        pList.getSelectedItems().forEach(x -> {
            var fd = FloatDock.getDock().findObject(x);
            fd.ifPresent(b -> FloatDock.getDock().remove(b));
            Profiler.getProfiler().deleteProfile(x);
        });

        pList.getItems().removeAll(pList.getSelectedItems());

        pList.setSelectionMode(false);
    }

    private void exportSelectedProfiles(){
        ProfileUtil.export(pList.getSelectedItems().stream().toList(), pList.getScene().getWindow());
        pList.setSelectionMode(false);
    }

    private void backupSelectedProfiles(){
        ProfileUtil.backup(pList.getSelectedItems().stream().toList(), pList.getScene().getWindow());
        pList.setSelectionMode(false);
    }

    private void close(Result res){
        setResult(res);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        close();
    }

    public Optional<Result> show(List<String> groups, List<Profile> profiles){
        this.groups = groups;
        this.profiles = profiles;

        cGroups.setValue(null);
        cGroups.getSelectionModel().clearSelection();

        pList.deselectAll();

        if (fn == Functionality.MULTIPLE_PROFILE_SELECTOR){
            pList.enableForceSelectionMode();
        }
        else{
            pList.setSelectionMode(false);
        }

        if (groups != null){
            cGroups.getItems().setAll(Translator.translate("dock.menu.group.create"));
            cGroups.getItems().addAll(groups);
        }

        pList.getItems().setAll(profiles);
        pList.load();

        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.YES);

        return super.action();
    }

}
