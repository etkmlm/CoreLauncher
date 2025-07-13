package com.laeben.corelauncher.ui.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Cursor;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class CCombo<T> extends Region {
    private static final PseudoClass OPENED =  PseudoClass.getPseudoClass("opened");

    private final ObservableList<T> items;

    private T selectedItem;

    private final CPopup popup;

    private final ListView<T> lvItems;

    private final TextField txtSearch;


    private boolean isOpen;
    private boolean attendedAuto;
    private boolean isSearching;

    private Consumer<T> onItemChanged;
    private Function<T, String> valueFactory;

    public CCombo(){
        this(null);
    }

    public CCombo(String listViewStyleClass){
        txtSearch = new TextField();
        lvItems = new ListView<>();
        popup = new CPopup();
        popup.setDuration(200);
        popup.setDirection(true);

        items = FXCollections.observableArrayList();

        isOpen = false;

        getStyleClass().add("ccombo");

        lvItems.getStyleClass().add("ccombo-list");
        if (listViewStyleClass != null)
            lvItems.getStyleClass().addAll(listViewStyleClass);
        lvItems.setItems(items);
        popup.setContent(lvItems);
        toggleVisibility(false);

        popup.addEventFilter(KeyEvent.KEY_PRESSED, a -> {
            if (a.getCode() == KeyCode.ESCAPE)
                toggleVisibility(false);

            // i had to patch regular text field shortcuts due to list view's behavior

            if (a.isControlDown() && a.getCode() == KeyCode.A){
                txtSearch.selectAll();
                a.consume();
            }
            if (a.getCode() == KeyCode.LEFT){
                txtSearch.positionCaret(txtSearch.getCaretPosition() < 1 ? 0 : txtSearch.getCaretPosition() - 1);
                a.consume();
            }
            else if (a.getCode() == KeyCode.RIGHT){
                int pos = txtSearch.getCaretPosition(), textLen = txtSearch.getText().length();
                txtSearch.positionCaret(pos >= textLen ? textLen - 1 : pos + 1);
                a.consume();
            }
        });

        lvItems.getSelectionModel().selectedItemProperty().addListener((a , o, n) -> {
            if (!attendedAuto)
                toggleVisibility(false);

            setValueInner(n);
        });

        txtSearch.borderProperty().bind(borderProperty());
        txtSearch.textProperty().addListener((a, o, n) -> {
            if (!isOpen)
                return;

            if (n == null || n.isBlank()){
                if (isSearching)
                    attendedAuto = true;
                lvItems.setItems(items);
                if (selectedItem != null && isSearching){
                    lvItems.getSelectionModel().select(selectedItem);
                    attendedAuto = false;
                }
                isSearching = false;
            }
            else{
                isSearching = true;
                attendedAuto = true;
                lvItems.setItems(items.filtered(x -> itemString(x).toLowerCase(Locale.getDefault()).contains(txtSearch.getText().toLowerCase(Locale.getDefault()))));
                if (selectedItem != null){
                    lvItems.getSelectionModel().select(selectedItem);
                }
                attendedAuto = false;
                //lvItems.getFocusModel().focus(0);
            }
        });

        lvItems.setCellFactory(a -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                if (item == null)
                    empty = true;

                super.updateItem(item, empty);

                if (empty){
                    setText(null);
                    return;
                }

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
        this.isOpen = isOpen;
        txtSearch.clear();
        txtSearch.setEditable(isOpen);
        isSearching = false;

        if (!isOpen){
            txtSearch.setCursor(Cursor.DEFAULT);
            popup.hide();
            if (selectedItem != null)
                txtSearch.setText(selectedItemString());

        }
        else{
            var bounds = localToScreen(txtSearch.getLayoutBounds());
            //lvItems.setPrefWidth(txtSearch.getWidth());
            popup.setWidth(txtSearch.getWidth());
            popup.show(this, bounds.getMinX(), bounds.getMaxY() - 10);
            txtSearch.setCursor(Cursor.TEXT);
        }

        pseudoClassStateChanged(OPENED, isOpen);
    }

    public void setValueFactory(Function<T, String> factory){
        this.valueFactory = factory;
    }

    public void setOnItemChanged(Consumer<T> onItemChanged){
        this.onItemChanged = onItemChanged;
    }

    private String itemString(T item) {
        return valueFactory != null ? valueFactory.apply(item) : item.toString();
    }
    private String selectedItemString(){
        return itemString(selectedItem);
    }

    private void setValueInner(T value){
        if (value == null)
            return;
        selectedItem = value;
        if (!isSearching){
            txtSearch.setText(selectedItemString());
            if (onItemChanged != null)
                onItemChanged.accept(value);
        }
    }

    public void setValue(T value){
        attendedAuto = true;
        lvItems.getSelectionModel().select(value);
        attendedAuto = false;
    }

    public T getSelectedItem(){
        return selectedItem;
    }

    public ObservableList<T> getItems(){
        return items;
    }
}
