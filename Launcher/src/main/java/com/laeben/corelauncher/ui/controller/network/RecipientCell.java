package com.laeben.corelauncher.ui.controller.network;

import com.laeben.corelauncher.lan.entity.LANRecipient;
import com.laeben.corelauncher.ui.dialog.DProfileSharePanel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

public class RecipientCell extends ListCell<LANRecipient> {
    private final BooleanProperty selected;

    private final HBox root;
    private final Text txtName;

    private LANRecipient item;

    public RecipientCell(ExecutorService executor) {
        root = new HBox();
        selected = new SimpleBooleanProperty(false);
        selected.addListener((observable, oldValue, newValue) -> {

        });

        root.setOnMouseClicked(a -> {
            if (item == null) return;

            if (a.getButton() == MouseButton.PRIMARY){
                if (a.getClickCount() == 2){
                    final var result = new DProfileSharePanel(getScene().getWindow())
                            .pick(true, false);

                    if (result.isEmpty()) return;
                    final var profiles = result.get().targetProfiles();

                    /*for (var profile : profiles){
                        var path = profiles.get(0).getPath();
                        executor.submit(() -> {
                            try {
                                LANShare.getInstance().share(item, new FilePacket.ProfilePacket().serialize(path));
                            } catch (IOException e) {
                                Logger.getLogger().log(e);
                            }
                        });
                    }*/
                }
                else{
                    selected.setValue(!selected.getValue());
                }
            }
        });
        root.setAlignment(Pos.CENTER);
        root.setPrefHeight(64);
        root.setPrefWidth(64);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #202030; -fx-background-radius: 16px");

        txtName = new Text();
        txtName.setStyle("-fx-fill: white; -fx-font-size: 20pt;");

        root.getChildren().add(txtName);
    }

    @Override
    public void updateItem(LANRecipient item, boolean empty) {
        if (item == null || empty){
            setGraphic(null);
            return;
        }

        this.item = item;
        selected.setValue(false);

        try {
            Tooltip.install(root, new Tooltip(item.getNetworkIdentifier()));
        } catch (UnknownHostException ignored) {

        }
        txtName.setText(item.name());

        setGraphic(root);
    }
}
