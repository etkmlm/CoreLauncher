package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.ui.entity.filter.FilterPreset;
import com.laeben.corelauncher.ui.entity.filter.FilterSection;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.*;

public class CFilterPane extends VBox {
    private final List<FilterPreset> presets;

    private String loadedPreset;
    private String pinnedPreset;

    public CFilterPane() {
        presets = new ArrayList<>();
    }

    public String getLoadedPreset(){
        return loadedPreset;
    }

    public void setPreset(String id){
        presets.forEach(a -> a.setEnabledEventListening(a.id().equals(id) || a.id().equals(pinnedPreset)));

        getChildren().clear();
        getChildren().addAll(getPreset(pinnedPreset).sections());
        if (id != null){
            var preset = getPreset(id);

            if (preset != null){
                var sections = preset.sections();
                sections.forEach(FilterSection::invalideSelections);
                getChildren().addAll(sections);
            }
        }
        getChildren().add(new Rectangle(0, 10));
        addClearButton();

        loadedPreset = id;
    }

    public void setPinnedPreset(String id){
        pinnedPreset = id;
    }

    public FilterPreset getPreset(String id){
        return presets.stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
    }

    public void clearSections(){
        for (var preset : presets){
            if (!preset.id().equals(pinnedPreset))
                preset.sections().forEach(FilterSection::clearChoices);
        }
    }

    private void addClearButton(){
        var hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);

        var btnClear = new CButton();
        btnClear.getStyleClass().add("clear-button");
        btnClear.enableTransparentAnimation();
        btnClear.setText("\uD83D\uDDD1");
        btnClear.setPrefWidth(100);
        btnClear.setOnMouseClicked(a -> {
            presets.forEach(b -> b.sections().forEach(FilterSection::clearChoices));
            //setPreset(null);
        });
        hbox.getChildren().add(btnClear);

        getChildren().add(hbox);
    }

    public void addPreset(String id, FilterSection... sections){
        var ss = List.of(sections);
        var preset = new FilterPreset(id, ss);
        ss.forEach(a -> a.setPreset(preset));
        presets.add(preset);
    }

    public void dispose(){
        presets.forEach(FilterPreset::dispose);
        presets.clear();
        getChildren().clear();
    }
}
