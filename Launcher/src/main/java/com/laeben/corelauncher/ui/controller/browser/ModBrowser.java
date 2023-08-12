package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.ui.controls.CButton;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;

public abstract class ModBrowser {
    protected Profile profile;

    public static <T extends ModBrowser> LStage open(Profile p, T controller, String titleId){
        var mds = FXManager.getManager().applyStage("modbrowser", controller, Translator.translate("frame.title." + titleId));
        controller.profile = p;
        controller.reset();

        return mds;
    }
    public ModBrowser(){
        resources = FXCollections.observableArrayList();

        Profiler.getProfiler().getHandler().addHandler("mbrowser", a -> {
            if (!a.getKey().equals("profileUpdate"))
                return;

            search();
        }, true);
    }

    @FXML
    public ComboBox<String> cbMainCategories;
    @FXML
    public ComboBox<String> cbCategories;
    @FXML
    public ComboBox<String> cbSortBy;
    @FXML
    public ComboBox<String> cbSort;
    @FXML
    public ColumnConstraints cstMainCat;
    @FXML
    public ColumnConstraints cstCat;
    @FXML
    public ColumnConstraints cstSortBy;
    @FXML
    public ColumnConstraints cstSort;
    @FXML
    public TextField txtQuery;
    @FXML
    public CButton btnSearch;
    @FXML
    public ListView<LModLink> lvMods;

    protected final ObservableList<LModLink> resources;

    @FXML
    public void initialize(){
        lvMods.setCellFactory(x -> cellFactory());
        lvMods.setItems(resources);

        btnSearch.setOnMouseClicked(a -> search());

        init();

        txtQuery.setOnKeyPressed(a -> {
            if (a.getCode() != KeyCode.ENTER)
                return;

            search();
        });

        cbMainCategories.valueProperty().addListener(a -> {

            var value = cbMainCategories.getValue();

            if (value == null || value.isEmpty())
                return;

            cbCategories.getItems().clear();

            int index = cbMainCategories.getItems().indexOf(value);
            onMainCategorySelected(index);
        });
    }

    public abstract void reset();
    public abstract void search();
    public abstract void init();
    public abstract <T extends BMod> T cellFactory();

    public abstract void onMainCategorySelected(int index);

}
