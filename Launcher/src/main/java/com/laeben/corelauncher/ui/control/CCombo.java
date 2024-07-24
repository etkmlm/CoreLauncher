package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.CoreLauncherFX;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class CCombo<T> extends Region {

    private final ObservableList<T> items;

    private T selectedItem;

    private final CPopup popup;

    private final ListView<T> lvItems;

    private final TextField txtSearch;


    private boolean isOpen;

    private Consumer<T> onItemChanged;
    private Function<T, String> valueFactory;

    public CCombo(){
        txtSearch = new TextField();
        lvItems = new ListView<>();
        popup = new CPopup();
        popup.setDuration(200);
        popup.setDirection(true);

        items = FXCollections.observableArrayList();

        isOpen = false;

        getStyleClass().add("ccombo");

        lvItems.setItems(items);
        popup.setContent(lvItems);
        toggleVisibility(false);

        lvItems.getSelectionModel().selectedItemProperty().addListener(a -> {
            toggleVisibility(false);
            setValue(lvItems.getSelectionModel().getSelectedItem());
        });

        lvItems.getStylesheets().add(CoreLauncherFX.CLUI_CSS);

        txtSearch.textProperty().addListener(a -> {
            if (!isOpen)
                return;

            if (txtSearch.getText().isBlank())
                lvItems.setItems(items);
            else{
                lvItems.setItems(items.filtered(x -> x.toString().toLowerCase(Locale.getDefault()).contains(txtSearch.getText().toLowerCase(Locale.getDefault()))));
                lvItems.getFocusModel().focus(0);
            }
        });

        lvItems.setCellFactory(a -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                if (empty || item == null){
                    setText(null);
                    return;
                }

                super.updateItem(item, false);
                setText(valueFactory != null ? valueFactory.apply(item) : item.toString());

            }
        });

        txtSearch.setOnMousePressed(a -> toggleVisibility(!isOpen));

        widthProperty().addListener(a -> {
            txtSearch.setPrefWidth(getWidth());
            txtSearch.setPrefHeight(getHeight());
        });

        popup.setOnHiding(a -> toggleVisibility(false));
        getChildren().add(txtSearch);
    }

    public void toggleVisibility(boolean isOpen){
        txtSearch.clear();
        this.isOpen = isOpen;
        txtSearch.setEditable(isOpen);

        if (!isOpen){
            popup.hide();
            if (selectedItem != null)
                txtSearch.setText(selectedItemString());
        }
        else{
            var bounds = localToScreen(txtSearch.getLayoutBounds());
            lvItems.setPrefWidth(txtSearch.getWidth());
            popup.setWidth(txtSearch.getWidth());
            popup.show(txtSearch, bounds.getMinX(), bounds.getMaxY() + 5);
        }
    }

    public void setValueFactory(Function<T, String> factory){
        this.valueFactory = factory;
    }

    public void setOnItemChanged(Consumer<T> onItemChanged){
        this.onItemChanged = onItemChanged;
    }

    private String selectedItemString(){
        return valueFactory != null ? valueFactory.apply(selectedItem) : selectedItem.toString();
    }

    public void setValue(T value){
        if (value == null)
            return;
        selectedItem = value;
        txtSearch.setText(selectedItemString());
        if (onItemChanged != null)
            onItemChanged.accept(value);
    }

    public T getSelectedItem(){
        return selectedItem;
    }

    public ObservableList<T> getItems(){
        return items;
    }
}
