package com.laeben.corelauncher.ui.entity.monitor;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.lan.entity.LANRecipient;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ProfileShareMonitorData extends MonitorData{
    private final VBox root;

    private final String recipient;
    private final Path path;

    public ProfileShareMonitorData(Object key, LANRecipient recipient, Path path) {
        super(key);

        this.path = path;
        this.recipient = recipient.name();

        root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(4);

        final var lblName = new Label(recipient.name());
        lblName.setTextFill(Color.WHITE);
        root.getChildren().add(lblName);

        final var lblPath = new Label(this.path.getName());
        lblPath.setTooltip(new Tooltip(this.path.toString()));
        lblPath.setTextFill(Color.WHITE);
        lblPath.setCursor(Cursor.HAND);
        lblPath.setOnMouseClicked(event -> OSUtil.open(this.path.toFile()));
        root.getChildren().add(lblPath);
    }

    @Override
    public Node serializeMetadata() {
        return root;
    }
}
