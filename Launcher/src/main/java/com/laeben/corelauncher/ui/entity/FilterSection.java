package com.laeben.corelauncher.ui.entity;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FilterSection extends VBox {

    public enum Type{
        SINGLE, MULTIPLE
    }

    public record ClickEventArgs(FilterPreset preset, FilterSection section){

    }

    private FilterPreset preset;
    private final String title;
    private final String id;
    private final Type type;

    private final List<String> selected;
    private final ToggleGroup group;

    private final VBox choices;
    private FilterSection(String title, String id, Type type) {
        this.title = title;
        this.type = type;
        this.id = id;

        var titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");
        getChildren().add(new Rectangle(0, 10));
        getChildren().add(titleLabel);

        getChildren().add(new Rectangle(0, 10));

        choices = new VBox();

        getChildren().add(choices);

        selected = new ArrayList<>();

        group = new ToggleGroup();
        group.selectedToggleProperty().addListener(a -> {
            selected.clear();

            if (group.getSelectedToggle() != null)
                selected.add(((RadioButton)group.getSelectedToggle()).getId());
        });
    }

    public void setPreset(FilterPreset preset) {
        this.preset = preset;
    }

    public String getIdentifier(){
        return id;
    }

    private ButtonBase getButton(){
        if (type == Type.SINGLE){
            var btn = new RadioButton();
            btn.setToggleGroup(group);
            return btn;
        }
        else if (type == Type.MULTIPLE){
            var btn = new CheckBox();
            btn.selectedProperty().addListener(a -> {
                if (btn.isSelected())
                    selected.add(btn.getId());
                else
                    selected.remove(btn.getId());
            });
            return btn;
        }
        else
            return null;
    }

    public String getSelectedChoice(){
        return selected.isEmpty() ? null : selected.get(0);
    }

    public List<String> getSelectedChoices(){
        return selected;
    }

    public void selectChoice(String id){
        choices.getChildren().stream().filter(a -> a.getId().equals(id)).findFirst().ifPresent(a -> {
            if (a instanceof CheckBox chk)
                chk.setSelected(true);
            else if (a instanceof RadioButton rb)
                rb.setSelected(true);

            a.getOnMouseClicked().handle(null);
        });
    }

    public FilterSection addChoice(String title, String id, Consumer<ClickEventArgs> onClick){
        var btn = getButton();
        if (btn == null)
            return this;

        btn.setText(title);
        btn.setId(id);

        btn.setOnMouseClicked(a -> {
            if (onClick != null)
                onClick.accept(new ClickEventArgs(preset, this));
        });

        choices.getChildren().add(btn);

        return this;
    }

    public void resetChoices(){
        choices.getChildren().clear();
    }

    public void clearChoices(){
        if (type == Type.SINGLE)
            group.selectToggle(null);
        else if (type == Type.MULTIPLE)
            choices.getChildren().forEach(a -> ((CheckBox)a).setSelected(false));
    }

    public static FilterSection create(String title, String id, Type type) {
        return new FilterSection(title, id, type);
    }

    public String getTitle() {
        return title;
    }

    public Type getType() {
        return type;
    }
}
