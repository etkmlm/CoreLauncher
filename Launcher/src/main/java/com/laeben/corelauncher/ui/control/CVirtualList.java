package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.controller.cell.CVirtualCell;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class CVirtualList<T> extends VBox {
    public record Filter<T>(T input, String query) { }

    private final CNav nav;
    private final CStatusLabel<Integer> statusLabel;

    private final IntegerProperty countProperty;
    private final BooleanProperty nullModeEnabled;

    private final BooleanProperty selectionMode;
    private boolean forceSelectionMode = false;

    private final FilteredList<T> filteredItems;
    private final ObservableList<T> items;

    private Supplier<CVirtualCell<T>> cellFactory;
    private Predicate<Filter<T>> filterFactory;

    private final ListChangeListener<T> selectionChangeListener;
    private final MultipleSelectionModel<T> selectionModel;
    private final ListView<T> list;
    private final HBox nullPane;

    public CVirtualList(){
        items = FXCollections.observableArrayList();
        filteredItems = new FilteredList<>(items);

        selectionMode = new SimpleBooleanProperty(false);
        countProperty = new SimpleIntegerProperty();
        nullModeEnabled = new SimpleBooleanProperty(true);

        getStyleClass().add("clist");

        nav = new CNav();
        nav.addItem(Translator.translate("option.cancel"), "-shape-cancel", a -> setSelectionMode(false), 0);
        nav.addItem(Translator.translate("option.selectAll"), "-shape-check", a -> selectAll(), 0);
        nav.addItem(Translator.translate("option.deselectAll"), "-shape-uncheck", a -> deselectAll(), 0);

        statusLabel = nav.addLabel(0, 1);
        statusLabel.setTextFactory(a -> Translator.translateFormat("dock.popup.selected", a));
        statusLabel.setIsEmpty(a -> a == null || a <= 0);
        statusLabel.setDefaultValue(0);

        getChildren().add(nav);

        selectionChangeListener = a -> {
            if (!getSelectedItems().isEmpty() && !getSelectionMode() && statusLabel.getValue() == 0)
                setSelectionMode(true);
            statusLabel.setValue(getSelectedItems().size());
        };

        list = new ListView<>();
        list.getStyleClass().add("list");
        VBox.setVgrow(list, Priority.ALWAYS);
        setMaxHeight(Double.MAX_VALUE);
        selectionModel = list.getSelectionModel();
        selectionModel.getSelectedItems().addListener(selectionChangeListener);
        list.setItems(filteredItems);
        getChildren().add(list);

        nullPane = new HBox();
        nullPane.setAlignment(Pos.TOP_CENTER);
        nullPane.setMaxWidth(Double.MAX_VALUE);
        nullPane.setManaged(false);
        nullPane.setVisible(false);
        VBox.setVgrow(nullPane, Priority.ALWAYS);

        var nullLabel = new Label();
        nullLabel.setText("-O-");

        nullPane.getChildren().add(nullLabel);
        getChildren().add(nullPane);

        filteredItems.addListener((ListChangeListener<? super T>) a -> {
            nullMode(filteredItems.isEmpty());
            statusLabel.setValue(filteredItems.size());
            countProperty.set(filteredItems.size());
        });

        nav.enabledProperty().bind(selectionMode);
        nav.pad(5, 5, 5, 5);
        selectionMode.addListener(a -> {
            if (!getSelectionMode())
                deselectAll();
            else
                statusLabel.setValue(null);
        });
    }

    public int getCount(){
        return countProperty.get();
    }
    public ReadOnlyIntegerProperty countProperty(){
        return ReadOnlyIntegerProperty.readOnlyIntegerProperty(countProperty);
    }

    public void selectAll(){
        if (list.getSelectionModel() != null)
            list.getSelectionModel().selectAll();
    }

    public void deselectAll(){
        if (list.getSelectionModel() != null)
            list.getSelectionModel().clearSelection();
    }

    public void setSelectionEnabled(boolean value){
        list.setSelectionModel(value ? selectionModel : null);
    }
    public void enableForceSelectionMode(){
        if (forceSelectionMode)
            return;
        setSelectionMode(true);
        forceSelectionMode = true;

        nav.delItem(0, 0);
    }
    public boolean getSelectionMode(){
        return selectionMode.get();
    }
    public void setSelectionMode(boolean mode){
        if (list.getSelectionModel() != null && !forceSelectionMode)
            selectionMode.set(mode);
    }

    public ObservableList<T> getSelectedItems(){
        return list.getSelectionModel().getSelectedItems();
    }

    public ListView<T> getList(){
        return list;
    }

    public boolean onKeyEvent(KeyEvent e){
        boolean ca = e.isControlDown() && e.getCode() == KeyCode.A;
        boolean esc = e.getCode() == KeyCode.ESCAPE;

        if (ca)
            selectAll();
        else if (esc)
            deselectAll();

        return ca || esc;
    }

    public boolean getNullModeEnabled(){
        return nullModeEnabled.get();
    }
    public void setNullModeEnabled(boolean value){
        nullModeEnabled.set(value);
    }

    private void nullMode(boolean mode){
        if (!getNullModeEnabled()) return;

        nullPane.setManaged(mode);
        nullPane.setVisible(mode);
        list.setManaged(!mode);
        list.setVisible(!mode);
    }
    public void setFilterFactory(Predicate<Filter<T>> factory){
        this.filterFactory = factory;
    }
    public void filter(String text){
        if (filterFactory == null) return;
        //inFilterMode = text != null && !text.isEmpty();
        filteredItems.setPredicate(text == null || text.isEmpty() ? null : t -> filterFactory.test(new Filter<>(t, text)));
    }

    public CNav getNav(){
        return nav;
    }

    public int getFilteredSize(){
        return filteredItems.size();
    }

    public ObservableList<T> getItems() {
        return items;
    }
    public void setCellFactory(Supplier<CVirtualCell<T>> factory){
        list.setCellFactory((l) -> factory.get());
    }
}
