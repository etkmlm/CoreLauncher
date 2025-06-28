package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.ui.control.CField;
import com.laeben.corelauncher.ui.control.CList;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.cell.CTCell;
import com.laeben.corelauncher.ui.tutorial.entity.Tutorial;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;

import java.util.List;

public class TutorialsPage extends HandlerController {
    public static final String KEY = "pgtuto";
    @FXML
    private CField txtSearch;
    @FXML
    private CList<Tutorial> pList;
    @FXML
    private ScrollPane scroll;

    public TutorialsPage() {
        super(KEY);
    }

    public List<Tutorial> getOnlineTutorials() {
        List<Tutorial> tutorials = List.of();
        try {
            tutorials = Main.getMain().getInstructor().getOnlineTutorials();
        } catch (NoConnectionException ignored) {

        } catch (HttpException e) {
            Logger.getLogger().log(e);
        }

        return tutorials;
    }

    @Override
    public void preInit() {
        pList.setLoadLimit(20);
        pList.setSelectionEnabled(false);

        var locale = Configurator.getConfig().getLanguage();
        pList.setFilterFactory(a -> a.input().getTitle(locale).toLowerCase().contains(a.query().toLowerCase()));
        pList.setCellFactory(CTCell::new);
        pList.getItems().setAll(getOnlineTutorials());
        pList.load();

        scroll.vvalueProperty().addListener((a, b, c) -> {
            if (b.equals(c) || scroll.getVmax() > c.doubleValue())
                return;
            pList.load(false);
        });

        txtSearch.textProperty().addListener(a -> pList.filter(txtSearch.getText()));
    }
}
