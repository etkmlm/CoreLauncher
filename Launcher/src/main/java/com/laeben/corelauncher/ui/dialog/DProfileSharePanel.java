package com.laeben.corelauncher.ui.dialog;

import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.lan.LANShare;
import com.laeben.corelauncher.lan.entity.LANRecipient;
import com.laeben.corelauncher.lan.exception.InvalidKeyException;
import com.laeben.corelauncher.lan.profile.SharePreference;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.ui.control.CWorker;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DProfileSharePanel extends CDialog<SharePreference>{


    public static class LocalProfileCell extends ListCell<Profile> {
        private final HBox root;
        private final Text text;
        private final CView view;
        private Profile item;
        public LocalProfileCell(){
            root = new HBox();
            root.setAlignment(Pos.CENTER_LEFT);
            root.setSpacing(16);
            root.setPadding(new Insets(4, 4, 4, 4));

            view = new CView();
            view.setFitHeight(32);
            view.setFitWidth(32);
            view.setCornerRadius(32, 32, 8);
            root.getChildren().add(view);

            text = new Text();
            text.setStyle("-fx-fill: white;-fx-font-size: 16pt");
            root.getChildren().add(text);

            var region = new Region();
            region.maxWidth(Double.MAX_VALUE);
            HBox.setHgrow(region, Priority.ALWAYS);
            root.getChildren().add(region);

            var btnRemove = new CButton();
            btnRemove.setText("—");
            btnRemove.setStyle("-fx-background-color: transparent; -fx-font-size: 13pt");
            btnRemove.setOnMouseClicked(a -> getListView().getItems().remove(getItem()));
            root.getChildren().add(btnRemove);
        }

        @Override
        public void updateItem(Profile item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty){
                setGraphic(null);
                return;
            }

            this.item = item;

            this.text.setText(item.getName());
            this.view.setImageAsync(ImageUtil.getImage(item.getIcon(), 32, 32));

            setGraphic(root);
        }
    }

    public static class LocalRecipientCell extends ListCell<LANRecipient> {
        private final HBox root;
        private final Text text;
        private LANRecipient item;
        public LocalRecipientCell(){
            root = new HBox();
            root.setAlignment(Pos.CENTER_LEFT);
            root.setSpacing(16);
            root.setPadding(new Insets(4, 4, 4, 4));

            text = new Text();
            text.setStyle("-fx-fill: white;-fx-font-size: 16pt");
            root.getChildren().add(text);

            var region = new Region();
            region.maxWidth(Double.MAX_VALUE);
            HBox.setHgrow(region, Priority.ALWAYS);
            root.getChildren().add(region);

            var btnRemove = new CButton();
            btnRemove.setText("—");
            btnRemove.setStyle("-fx-background-color: transparent; -fx-font-size: 13pt");
            btnRemove.setOnMouseClicked(a -> getListView().getItems().remove(getItem()));
            root.getChildren().add(btnRemove);
        }

        @Override
        public void updateItem(LANRecipient item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty){
                setGraphic(null);
                return;
            }

            this.item = item;

            this.text.setText(item.getKey());

            setGraphic(root);
        }
    }

    public static class FlowRecipientCell extends VBox{
        private final LANRecipient item;

        public FlowRecipientCell(LANRecipient item, ObservableList<LANRecipient> list){
            this.item = item;

            try {
                Tooltip.install(this, new Tooltip(item.getNetworkIdentifier()));
            } catch (UnknownHostException ignored) {

            }
            setPrefWidth(96);
            setPrefHeight(96);
            setAlignment(Pos.CENTER);
            setSpacing(8);
            setStyle("-fx-padding: 16px; -fx-background-color: -list-background; -fx-background-radius: 16px");

            var text = new Text();
            text.setStyle("-fx-fill: white;-fx-font-size: 18pt; -fx-font-weight: bold");
            text.setText(item.toString());
            getChildren().add(text);

            var btnRemove = new CButton();
            btnRemove.setText("—");
            btnRemove.setStyle("-fx-background-color: transparent; -fx-font-size: 13pt");
            btnRemove.setOnMouseClicked(a -> {
                if (!LANShare.getInstance().removeRecipient(getRecipient().ip()))
                    list.remove(getRecipient());
            });
            btnRemove.enableTransparentAnimation();
            getChildren().add(btnRemove);
        }

        public LANRecipient getRecipient(){
            return item;
        }
    }

    public static final String KEY = "dpspanel";

    private final ObservableList<Profile> profiles;
    private boolean pickProfiles;
    private final ObservableList<LANRecipient> recipients;
    private boolean pickRecipients;

    private final String hostKey;

    @FXML
    private CButton btnClose;
    @FXML
    private CButton btnDone;
    @FXML
    private Label lblKey;

    @FXML
    private VBox pRecipients;
    @FXML
    private FlowPane paneRecepients;
    @FXML
    private TextField txtKey;
    @FXML
    private CButton btnAcceptKey;
    @FXML
    private CWorker<LANRecipient, ?> worker;
    @FXML
    private ListView<LANRecipient> lvRecipients;

    @FXML
    private VBox pProfiles;
    @FXML
    private CButton btnPickProfiles;
    @FXML
    private CheckBox chkFull;
    @FXML
    private ListView<Profile> lvProfiles;

    @FXML
    private ScrollPane scroll;

    private final Executor executor;

    private LANRecipient workerTargetRecipient;

    public DProfileSharePanel(Window owner) {
        super("layout/dialog/sharepanel.fxml", true, owner);
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);

        this.profiles = FXCollections.observableArrayList();
        this.recipients = FXCollections.observableArrayList();
        this.hostKey = LANShare.getInstance().generateKeyString();
        this.executor = Executors.newSingleThreadExecutor();
        lblKey.setText(this.hostKey);

        //lvRecipients.setItems(this.recipients);
        lvProfiles.setItems(this.profiles);

        this.recipients.addListener((ListChangeListener<LANRecipient>) c -> {
            if (!pickRecipients) return;

            while (c.next()){
                if (c.wasAdded()){
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        paneRecepients.getChildren().add(new FlowRecipientCell(c.getList().get(i), this.recipients));
                    }
                }
                else if (c.wasRemoved()){
                    final var hash = new HashSet<>(c.getRemoved());

                    paneRecepients.getChildren().removeIf(a -> hash.contains(((FlowRecipientCell)a).getRecipient()));
                }
            }

            btnDone.setDisable(c.getList().isEmpty() && (!pickProfiles || !this.profiles.isEmpty()));
        });
        //lvRecipients.prefHeightProperty().bind(Bindings.size(this.recipients).multiply(60).add(2));

        this.profiles.addListener((ListChangeListener<Profile>) c -> {
            if (!pickProfiles) return;

            btnDone.setDisable(this.profiles.isEmpty() && (!pickRecipients || !this.recipients.isEmpty()));
            lvProfiles.setVisible(!this.profiles.isEmpty());
        });
        lvProfiles.prefHeightProperty().bind(Bindings.size(this.profiles).multiply(60).add(2));
    }

    private void acceptKey(){
        if (!worker.isRunning())
            worker.run();
    }

    @FXML
    private void initialize(){
        btnClose.enableTransparentAnimation();
        btnClose.setOnMouseClicked(a -> {
            setResult(null);
            close();
        });

        btnDone.setOnMouseClicked(a -> {
            setResult(new SharePreference(
                    profiles.isEmpty() ? null : java.util.Set.copyOf(profiles),
                    chkFull.isSelected(),
                    recipients.isEmpty() ? null : java.util.Set.copyOf(recipients)
            ));
            close();
        });

        //lvRecipients.setCellFactory(a -> new LocalRecipientCell());
        txtKey.textProperty().addListener((a, b, text) -> {
            final boolean enabled = text != null && !text.isEmpty();
            btnAcceptKey.setManaged(enabled);
            btnAcceptKey.setVisible(enabled);
        });
        txtKey.setOnKeyPressed(event -> {
            if (event.getCode() != KeyCode.ENTER) return;

            acceptKey();
        });
        btnAcceptKey.setOnMouseClicked(a -> acceptKey());

        lvProfiles.setCellFactory(a -> new LocalProfileCell());
        btnPickProfiles.setOnMouseClicked(a -> {
            var hash = new HashSet<>(this.profiles);
            var selector = new DProfileSelector(DProfileSelector.Functionality.MULTIPLE_PROFILE_SELECTOR, getOwner());

            final var result = selector.show(null, Profiler.getProfiler().getAllProfiles().stream().filter(p -> !hash.contains(p)).toList());
            if (result.isEmpty()) return;

            profiles.addAll(result.get().getProfiles());
        });

        lblKey.setCursor(Cursor.HAND);
        lblKey.setOnMouseClicked(a -> {
            if (a.getButton() != MouseButton.PRIMARY) return;

            OSUtil.setClipboard(this.hostKey);
            Main.getMain().announceLater(Translator.translate("announce.successful"), Translator.translate("announce.info.share.copied"), Announcement.AnnouncementType.INFO, Duration.seconds(2));
        });

        worker.begin().withExecutor(executor).withTask(w -> new Task<>() {
            @Override
            protected LANRecipient call() throws Exception {
                UI.runAsync(() -> btnAcceptKey.setDisable(true));
                LANRecipient recipient = LANRecipient.fromKey(txtKey.getText());

                if (recipients.contains(recipient)) {
                    txtKey.clear();
                    return null;
                }
                return LANShare.getInstance().syncRecipient(recipient, null);
            }
        }).onFailed(a -> {
            if (a.getError() instanceof InvalidKeyException){
                // nothing
            }
            else if (a.getError() instanceof ConnectException){
                // nothing
            }
            else if (a.getError() instanceof IOException e){
                Logger.getLogger().log(e);
            }
        }).onDone(a -> {
            if (a.getValue() != null) recipients.add(a.getValue());

            txtKey.clear();
        }).finallyDo(a -> btnAcceptKey.setDisable(false));
    }

    public Optional<SharePreference> pick(boolean pickProfiles, boolean pickRecipients){
        this.pickRecipients = pickRecipients;
        this.pickProfiles = pickProfiles;

        this.profiles.clear();
        this.recipients.clear();

        pRecipients.setVisible(pickRecipients);
        pRecipients.setManaged(pickRecipients);
        btnAcceptKey.setVisible(false);
        btnAcceptKey.setManaged(false);

        lvProfiles.setVisible(false);
        pProfiles.setVisible(pickProfiles);
        pProfiles.setManaged(pickProfiles);

        //lblKey.setVisible(pickProfiles && pickRecipients);
        //lblKey.setManaged(pickProfiles && pickRecipients);

        btnDone.setDisable(true);

        if (pickRecipients){
            LANShare.getInstance().getHandler().addHandler(KEY + hashCode(), a -> {
                if (a instanceof ValueEvent ve){
                    switch (ve.getKey()){
                        case LANShare.NEW_RECIPIENT -> {
                            var recp = (LANRecipient) ve.getValue();

                            recipients.add(recp);
                        }
                        case LANShare.REMOVE_RECIPIENT -> {
                            var recp = (LANRecipient) ve.getValue();

                            recipients.remove(recp);
                        }
                    }
                }
            }, true);
        }

        final var result = action();

        if (pickRecipients){
            LANShare.getInstance().getHandler().removeHandler(KEY + hashCode());
        }

        return result;
    }
}
