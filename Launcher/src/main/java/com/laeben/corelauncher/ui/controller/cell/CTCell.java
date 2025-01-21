package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.tutorial.entity.Tutorial;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class CTCell extends CCell<Tutorial> {

    private Tutorial tutorial;

    public CTCell() {
        super("layout/cells/tutocell.fxml");
    }

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblDescription;
    @FXML
    private CButton btnStart;

    @Override
    public CCell setItem(Tutorial item) {
        this.tutorial = item;

        var lang = Configurator.getConfig().getLanguage();

        lblTitle.setText(item.getTitle(lang));
        lblDescription.setText(item.getDescription(lang));

        btnStart.setOnMouseClicked(a -> {
            Main.getMain().getTab().getSelectionModel().select(0);
            Main.getMain().getInstructor().load(tutorial);
            Main.getMain().getInstructor().start(true);
        });

        super.getChildren().clear();
        super.getChildren().add(node);
        return this;
    }

    @Override
    public Tutorial getItem() {
        return tutorial;
    }
}
