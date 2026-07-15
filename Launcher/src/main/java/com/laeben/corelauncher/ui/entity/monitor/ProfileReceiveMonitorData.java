package com.laeben.corelauncher.ui.entity.monitor;

import com.laeben.core.entity.Path;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ProfileReceiveMonitorData extends MonitorData{
    private final VBox root;

    public ProfileReceiveMonitorData(Path path) {
        super(path);

        root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(4);

        final var lblPath = new Label(path.getName());
        lblPath.setTooltip(new Tooltip(path.toString()));
        lblPath.setTextFill(Color.WHITE);
        root.getChildren().add(lblPath);
    }

    @Override
    public Node serializeMetadata() {
        return root;
    }
}
