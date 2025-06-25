package com.laeben.corelauncher.ui.entity.filter;

import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.control.CButton;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.controlsfx.control.SearchableComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FilterSection<T> extends VBox {

    public interface FilterNodeScheme<T extends Node>{
        FilterNodeScheme RADIO_BUTTON = new FilterNodeScheme<RadioButton>() {
            public static final RadioButton NULL_TOGGLE = new RadioButton();
            @Override
            public void setNodeSelected(RadioButton c, boolean value, FilterSection section) {
                if (c.equals(section.toggleGroup.getSelectedToggle())){
                    if (!value){
                        section.toggleGroup.selectToggle(NULL_TOGGLE);
                    }
                    /*if (!value && section.defaultChoice != null){
                        section.vbChoices.getChildren().stream().filter(a -> a.getId().equals(section.defaultChoice)).findFirst().ifPresent(x -> section.toggleGroup.selectToggle((RadioButton) x));
                    }*/
                }
                else if (value || (c.getId() != null && c.getId().equals(section.defaultChoice)))
                    section.toggleGroup.selectToggle(c);
            }

            @Override
            public RadioButton createNode(String id, String title, FilterSection section) {
                var rb = new RadioButton();
                if (!section.toggleGroup.getToggles().contains(NULL_TOGGLE))
                    section.toggleGroup.getToggles().add(NULL_TOGGLE);
                rb.setToggleGroup(section.toggleGroup);
                rb.setId(id);
                rb.setText(title);
                return rb;
            }

            @Override
            public boolean doesExistanceMeansChecked() {
                return false;
            }
        };
        FilterNodeScheme CHECKBOX = new FilterNodeScheme<CheckBox>() {
            @Override
            public void setNodeSelected(CheckBox c, boolean value, FilterSection section) {
                c.setSelected(value);
            }

            @Override
            public CheckBox createNode(String id, String title, FilterSection section) {
                var chk = new CheckBox();

                chk.setId(id);
                chk.setText(title);

                chk.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && newValue){
                        section.selected.add(id);
                        section.fireActionEvent(chk);
                    }
                    else{
                        section.selected.remove(id);
                    }

                });

                return chk;
            }

            @Override
            public boolean doesExistanceMeansChecked() {
                return false;
            }
        };
        FilterNodeScheme COMBOCELL = new FilterNodeScheme<HBox>() {

            @Override
            public void setNodeSelected(HBox c, boolean value, FilterSection section) {
                if (value){
                    section.selected.add(c.getId());
                    section.vbChoices.getChildren().add(c);
                    section.fireActionEvent(c.getId());
                }
                else{
                    section.selected.remove(c.getId());
                    section.vbChoices.getChildren().remove(c);
                }
            }

            @Override
            public HBox createNode(String id, String title, FilterSection section) {
                var hbox = new HBox();
                hbox.setAlignment(Pos.CENTER);
                hbox.setId(id);
                hbox.getStyleClass().add("filter-combo-choice");

                var label = new Label();
                HBox.setHgrow(label, Priority.ALWAYS);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setText(title);

                var btn = new CButton();
                btn.getStyleClass().add("transparent");
                btn.enableTransparentAnimation();
                btn.setText("—");
                btn.setOnMouseClicked(a -> setNodeSelected(hbox, false, section));

                hbox.getChildren().addAll(label, btn);

                return hbox;
            }

            @Override
            public boolean doesExistanceMeansChecked() {
                return true;
            }
        };

        void setNodeSelected(T c, boolean value, FilterSection section);
        T createNode(String id, String title, FilterSection section);
        boolean doesExistanceMeansChecked();
    }

    public enum FilterType{
        SINGLE_RADIO(FilterNodeScheme.RADIO_BUTTON), MULTIPLE_CHECKBOX(FilterNodeScheme.CHECKBOX), SINGLE_COMBO(FilterNodeScheme.COMBOCELL), MULTIPLE_COMBO(FilterNodeScheme.COMBOCELL);

        private final FilterNodeScheme scheme;

        FilterType(FilterNodeScheme scheme) {
            this.scheme = scheme;
        }

        public <T extends Node> FilterNodeScheme<T> getScheme() {
            return (FilterNodeScheme<T>) scheme;
        }
    }

    public record ActionEventArgs<T>(FilterPreset preset, FilterSection section, String choiceId, T state){

    }

    private FilterPreset preset;
    private final String title;
    private final String id;
    private final FilterType type;

    private final List<String> selected;

    private final ToggleGroup toggleGroup;

    private final List<T> states;

    private final VBox vbChoices;

    private SearchableComboBox<String> comboBox;
    private List<String> comboIds;

    private boolean singleton;

    private String defaultChoice;
    private boolean usesState = true;
    private Consumer<ActionEventArgs<T>> onAction;

    private FilterSection(String title, String id, FilterType type) {
        this.title = title;
        this.type = type;
        this.id = id;

        states = new ArrayList<>();

        selected = new ArrayList<>();

        var titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");
        getChildren().add(new Rectangle(0, 10));
        getChildren().add(titleLabel);

        getChildren().add(new Rectangle(0, 10));

        vbChoices = new VBox();

        if (type.getScheme().doesExistanceMeansChecked()){
            comboBox = new SearchableComboBox<>();
            comboIds = new ArrayList<>();

            vbChoices.setSpacing(10);

            comboBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.equals(-1))
                    return;

                var t = comboBox.getItems().get(newValue.intValue());
                var i = comboIds.get(newValue.intValue());

                UI.runAsync(() -> {
                    comboBox.setValue(null);
                    comboBox.getSelectionModel().clearSelection();
                });

                if (selected.contains(i))
                    return;

                type.getScheme().setNodeSelected(type.getScheme().createNode(i, t, this), true, this);
            });
            getChildren().addAll(comboBox, new Rectangle(0, 10));
        }

        getChildren().add(vbChoices);

        toggleGroup = new ToggleGroup();
        toggleGroup.selectedToggleProperty().addListener((a, b, c) -> {
            if (b != null)
                selected.remove(((RadioButton)b).getId());

            if (c instanceof RadioButton rb){
                selected.add(rb.getId());
                fireActionEvent(rb);
            }
        });
    }

    public void setPreset(FilterPreset preset) {
        this.preset = preset;
    }

    public String getIdentifier(){
        return id;
    }

    public String getSelectedChoice(){
        return selected.isEmpty() ? null : selected.get(0);
    }

    public List<String> getSelectedChoices(){
        return selected.stream().toList();
    }

    public T getState(String choiceId){
        if (!type.getScheme().doesExistanceMeansChecked()){
            for (int i = 0; i < vbChoices.getChildren().size(); i++) {
                var c = vbChoices.getChildren().get(i);
                if (c.getId().equals(choiceId)) {
                    return states.get(i);
                }
            }
            return null;
        }

        int index = comboIds.indexOf(choiceId);
        return states.get(index);
    }

    public FilterSection<T> setSingleton(boolean singleton) {
        this.singleton = singleton;
        return this;
    }

    public void selectChoice(String id){
        if (type.getScheme().doesExistanceMeansChecked()){
            int ix = comboIds.indexOf(id);
            if (ix != -1)
                comboBox.getSelectionModel().select(ix);
            return;
        }

        for (var c : vbChoices.getChildren()){
            if (!c.getId().equals(id))
                continue;

            type.getScheme().setNodeSelected(c, true, this);
        }
    }

    public FilterSection<T> useState(boolean value){
        usesState = value;

        if (!value)
            states.clear();
        return this;
    }

    public FilterSection<T> addChoice(String title, String id){
        return addChoice(title, id, null);
    }
    public FilterSection<T> addChoice(String title, String id, T state){
        if (usesState)
            states.add(state);

        if (!type.getScheme().doesExistanceMeansChecked())
            vbChoices.getChildren().add(type.getScheme().createNode(id, title, this));
        else{
            comboIds.add(id);
            comboBox.getItems().add(title);
        }

        return this;
    }

    public FilterSection<T> defaultChoice(String id){
        defaultChoice = id;
        return this;
    }

    public FilterSection<T> setOnAction(Consumer<ActionEventArgs<T>> onAction){
        this.onAction = onAction;
        return this;
    }

    private void fireActionEvent(Node node){
        if (onAction != null && preset.isEnabledEventListening())
            onAction.accept(new ActionEventArgs<>(preset, this, node.getId(), usesState ? states.get(vbChoices.getChildren().indexOf(node)) : null));
    }

    private void fireActionEvent(String choiceId, T state){
        if (onAction != null && preset.isEnabledEventListening())
            onAction.accept(new ActionEventArgs<>(preset, this, choiceId, state));
    }

    private void fireActionEvent(String choiceId){
        T state = null;

        if (usesState){
            state = getState(choiceId);
        }

        fireActionEvent(choiceId, state);
    }

    private void fireActionEvent(int choiceIndex){
        String choiceId;
        if (!type.getScheme().doesExistanceMeansChecked())
            choiceId = vbChoices.getChildren().get(choiceIndex).getId();
        else
            choiceId = comboIds.get(choiceIndex);

        fireActionEvent(choiceId, usesState ? states.get(choiceIndex) : null);
    }

    public void resetChoices(){
        selected.clear();
        states.clear();
        if (type.getScheme().doesExistanceMeansChecked()){
            //comboBox.getItems().clear();
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
            comboIds.clear();
        }
        vbChoices.getChildren().clear();
    }

    public void reselectChoices(){
        if (selected.isEmpty())
            return;
        for (var c : vbChoices.getChildren()){
            if (!selected.contains(c.getId()))
                continue;
            type.getScheme().setNodeSelected(c, false, this);
            type.getScheme().setNodeSelected(c, true, this);
        }
    }

    public void invalideSelections(){
        if (selected.isEmpty())
            return;
        for (var c : selected){
            fireActionEvent(c);
        }
    }

    public void clearChoices(){
        vbChoices.getChildren().stream().toList().forEach(a -> type.getScheme().setNodeSelected(a, false, this));
    }

    public static <T> FilterSection<T> create(String title, String id, FilterType type) {
        return new FilterSection(title, id, type);
    }

    public String getTitle() {
        return title;
    }

    public FilterType getType() {
        return type;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public boolean validateDefaults(){
        if (defaultChoice != null && selected.isEmpty()){
            selectChoice(defaultChoice);
            return true;
        }
        return false;
    }
}
