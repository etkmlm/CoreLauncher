package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.socket.packet.FilePacket;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.lan.LANShare;
import com.laeben.corelauncher.lan.entity.LANRecipient;
import com.laeben.corelauncher.lan.exception.InvalidKeyException;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.network.RecipientCell;
import com.laeben.corelauncher.ui.util.ProfileUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkBrowserPage extends HandlerController {
    public static final String KEY = "pgnbrowser";

    @FXML
    private Label lblKey;
    @FXML
    private TextField txtKey;
    @FXML
    private ListView<LANRecipient> lvRecipients;

    private final ObservableList<LANRecipient> recipients;
    private final ExecutorService executor;

    public NetworkBrowserPage() {
        super(KEY);

        recipients = FXCollections.observableArrayList(LANShare.getInstance().getStoredRecipients());
        executor = Executors.newFixedThreadPool(10);

        registerHandler(LANShare.getInstance().getHandler(), a -> {
            if (a instanceof ValueEvent ve){
                switch (ve.getKey()) {
                    case LANShare.NEW_RECIPIENT -> {
                        var recp = (LANRecipient) ve.getValue();

                        recipients.add(recp);
                    }
                    case LANShare.REMOVE_RECIPIENT -> {
                        var recp = (LANRecipient) ve.getValue();

                        recipients.remove(recp);
                    }
                    case LANShare.FILE_RECEIVED -> {
                        var packet = (FilePacket) ve.getValue();

                        if (packet instanceof FilePacket.ProfilePacket pp) {
                            Main.getMain().announceLater("Yay!", "Received a profile which is now located in: " + pp.getTempPath().toString(), Announcement.AnnouncementType.INFO, Duration.seconds(2));
                            executor.execute(() -> {
                                try{
                                    ProfileUtil.importO(pp.getTempPath(), 0, 0);
                                }
                                catch (CancellationException ignored){

                                }
                            });
                        }
                    }
                }
            }
        }, true);
    }

    @Override
    public void preInit() {
        lblKey.setText(LANShare.getInstance().generateKeyString());
        lblKey.setCursor(Cursor.HAND);
        lblKey.setOnMouseClicked(a -> {
            OSUtil.setClipboard(lblKey.getText());
            Main.getMain().announceLater(Translator.translate("announce.successful"), Translator.translate("announce.info.share.copied"), Announcement.AnnouncementType.INFO, Duration.seconds(2));
        });

        lvRecipients.setCellFactory(a -> new RecipientCell(executor));
        lvRecipients.setItems(recipients);

        txtKey.setOnKeyPressed(a -> {
            if (a.getCode() != KeyCode.ENTER) return;

            LANRecipient recipient;

            try{
                recipient = LANRecipient.fromKey(txtKey.getText());
            }
            catch (InvalidKeyException e){
                Main.getMain().announceLater(Translator.translate("error.oops"), "", Announcement.AnnouncementType.ERROR, Duration.seconds(2));
                return;
            }



            txtKey.clear();
        });
    }
}
