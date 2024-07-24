package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.controller.cell.CCell;
import com.laeben.corelauncher.ui.entity.CLSelectable;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CList<T> extends VBox {
    public record Filter<T>(T input, String query){

    }

    @FunctionalInterface
    public interface Equator<T> {
        boolean equals(T first, T second);
    }

    private final CNav nav;
    private final CStatusLabel<Integer> statusLabel;

    private final BooleanProperty selectionMode;
    private boolean selectionEnabled = true;
    private boolean forceSelectionMode = false;

    private final ObservableList<T> items;
    private final ObservableList<T> selectedItems;

    private Supplier<CCell<T>> cellFactory;
    private Predicate<Filter<T>> filterFactory;
    private Equator<T> itemEqualsFactory;
    private final VBox list;
    private final HBox nullPane;

    private int lastLoadedIndex = -1;
    private int loadLimit = -1;

    public CList() {
        items = FXCollections.observableArrayList();
        selectedItems = FXCollections.observableArrayList();

        selectionMode = new SimpleBooleanProperty(false);

        nav = new CNav();
        nav.addItem("X " + Translator.translate("option.cancel"), a -> setSelectionMode(false), 0);
        nav.addItem("☑ " + Translator.translate("option.selectAll"), a -> selectAll(), 0);
        nav.addItem("☐ " + Translator.translate("option.deselectAll"), a -> deselectAll(), 0);

        //var r = nav.generateRow();
        //r.setAlignment(Pos.CENTER_RIGHT);
        //r.setPadding(new Insets(0, 16, 0, 0));

        statusLabel = nav.addLabel(0, 1);
        statusLabel.setTextFactory(a -> Translator.translateFormat("dock.popup.selected", a));
        statusLabel.setIsEmpty(a -> a == null || a <= 0);
        statusLabel.setDefaultValue(0);

        getChildren().add(nav);

        list = new VBox();
        VBox.setVgrow(list, Priority.ALWAYS);
        setMaxHeight(Double.MAX_VALUE);
        getChildren().add(list);

        nullPane = new HBox();
        nullPane.setAlignment(Pos.CENTER);
        nullPane.setMaxWidth(Double.MAX_VALUE);
        nullPane.setManaged(false);
        nullPane.setVisible(false);
        VBox.setVgrow(nullPane, Priority.ALWAYS);

        var nullLabel = new Label();
        nullLabel.setText("-O-");

        nullPane.getChildren().add(nullLabel);
        getChildren().add(nullPane);

        items.addListener((ListChangeListener<? super T>) a -> {
            while (a.next()){
                if (a.wasUpdated() || (a.wasAdded() && a.wasRemoved())){
                    for (int i = 0; i < list.getChildren().size(); i++){
                        for (int j = a.getFrom(); j < a.getTo(); j++){
                            var item = items.get(j);
                            var f = (CCell<T>)list.getChildren().get(i);
                            if (itemEqualsFactory == null ? f.getItem().equals(item) : itemEqualsFactory.equals(f.getItem(), item)){
                                list.getChildren().set(i, getCell(item));
                            }
                        }
                    }
                }
                else if (a.wasAdded()){
                    if (loadLimit == -1){
                        for (int i = a.getFrom(); i < a.getTo(); i++)
                            list.getChildren().add(getCell(items.get(i)));
                    }
                }
                else if (a.wasRemoved()){
                    selectedItems.removeAll(a.getRemoved());
                    var remove = new ArrayList<CCell<T>>();
                    for (int i = 0; i < list.getChildren().size(); i++){
                        var f = (CCell<T>) list.getChildren().get(i);
                        if (a.getRemoved().contains(f.getItem()))
                            remove.add(f);
                    }
                    list.getChildren().removeAll(remove);
                }

                nullMode(items.isEmpty());
            }
        });

        selectedItems.addListener((ListChangeListener<? super T>) a -> {
            if (!selectedItems.isEmpty() && !getSelectionMode() && statusLabel.getValue() == 0)
                setSelectionMode(true);
            statusLabel.setValue(selectedItems.size());
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

    public ObservableList<T> getItems() {
        return items;
    }
    public void setCellFactory(Supplier<CCell<T>> factory){
        cellFactory = factory;
    }

    public void setItemEqualsFactory(Equator<T> factory){
        this.itemEqualsFactory = factory;
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

    private CCell<T> getCell(T item){
        var cell = cellFactory.get().setItem(item).setList(this);
        if (selectionEnabled && cell instanceof CLSelectable cls){
            if (selectedItems.contains(item))
                cls.setSelected(true);
            cls.setSelectionListener(a -> {
                if (a)
                    selectedItems.add(item);
                else
                    selectedItems.remove(item);
            });
        }
        return cell;
    }
    public void reload(boolean considerLimit){
        list.getChildren().clear();
        Platform.runLater(() -> {
            int lls = list.getChildren().size();
            int size = items.size();
            list.getChildren().clear();
            if (items.isEmpty()){
                nullMode(true);
                return;
            }
            for (int i = 0; (!considerLimit || i < lls) && i < size; i++)
                list.getChildren().add(getCell(items.get(i)));
        });
    }
    public void setLoadLimit(int limit){
        loadLimit = limit;

        if (limit == -1)
            reload(false);
    }
    public void load(){
        if (loadLimit == -1){
            reload(false);
            return;
        }

        if (lastLoadedIndex >= items.size()){
            reload(false);
            return;
        }

        var it = new ArrayList<CCell<T>>();
        int i;
        for (i = lastLoadedIndex + 1; i < lastLoadedIndex + 1 + loadLimit && i < items.size(); i++){
            it.add(getCell(items.get(i)));
        }

        lastLoadedIndex = i - 1;

        Platform.runLater(() ->{
            list.getChildren().addAll(it);
            nullMode(list.getChildren().isEmpty());
        });
    }

    private void nullMode(boolean mode){
        nullPane.setManaged(mode);
        nullPane.setVisible(mode);
    }
    public void setFilterFactory(Predicate<Filter<T>> factory){
        this.filterFactory = factory;
    }
    public void filter(String text){
        if (filterFactory == null)
            return;

        if (text == null || text.isBlank()){
            list.getChildren().clear();
            lastLoadedIndex = -1;
            load();
            return;
        }

        Platform.runLater(() -> {
            list.getChildren().setAll(items.stream().filter(a -> filterFactory.test(new Filter<>(a, text))).map(this::getCell).toList());
            nullMode(list.getChildren().isEmpty());
        });
    }

    public BooleanProperty selectionModeProperty(){
        return selectionMode;
    }
    public void setSelectionMode(boolean val){
        if (selectionEnabled && !forceSelectionMode)
            selectionMode.set(val);
    }
    public boolean getSelectionMode(){
        return selectionMode.get();
    }

    public void enableForceSelectionMode(){
        if (forceSelectionMode)
            return;
        setSelectionMode(true);
        forceSelectionMode = true;

        nav.delItem(0, 0);
    }

    public void setSelectionEnabled(boolean e){
        selectionEnabled = e;
    }

    public ObservableList<T> getSelectedItems(){
        return selectedItems;
    }

    public void selectAll(){
        list.getChildren().forEach(a -> {
            if (a instanceof CLSelectable cls)
                cls.setSelected(true);
        });
        if (items.size() != selectedItems.size())
            selectedItems.setAll(items.stream().toList());
    }

    public VBox getList(){
        return list;
    }

    public CNav getNav(){
        return nav;
    }

    public void deselectAll(){
        list.getChildren().forEach(a -> {
            if (a instanceof CLSelectable cls)
                cls.setSelected(false);
        });
        if (!selectedItems.isEmpty())
            selectedItems.clear();
    }
}
