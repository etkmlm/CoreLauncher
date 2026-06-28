package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.api.Translator;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.Consumer;

public class SelectionPane<T> extends Pane {
    private final Rectangle selectRect;
    private final CNav nav;
    private final Pane navMaster;

    private Consumer<Bounds> onSelected;
    private Consumer<Point2D> onSelectBegin;
    private Runnable onSelectCancel;

    private boolean rectMode = false;
    private double rectStartX, rectStartY;

    private final ObservableList<T> selectedItems;

    public SelectionPane(){
        selectedItems = FXCollections.observableArrayList();

        selectRect = new Rectangle();
        selectRect.setViewOrder(-1);
        selectRect.getStyleClass().add("select-rect");

        getChildren().add(selectRect);

        navMaster = new Pane();
        navMaster.setViewOrder(-2);

        nav = new CNav();
        nav.addItem(Translator.translate("option.cancel"), "-shape-cancel", a -> {
            if (onSelectCancel != null)
                onSelectCancel.run();
        }, 0);

        var row = nav.generateRow();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(0, 16, 10, 0));
        var selectedLabel = nav.addLabel(0, 1);
        selectedLabel.getStyleClass().add("selected-label");
        selectedLabel.setDefaultValue(0);
        selectedLabel.setIsEmpty(a -> a == null || a <= 0);
        selectedLabel.setTextFactory(a -> Translator.translateFormat("dock.popup.selected", a));

        navMaster.getChildren().add(nav);
        getChildren().add(navMaster);

        setOnMouseReleased(a -> {
            if (!rectMode)
                return;

            rectStartX = rectStartY = 0;
            selectRect.setVisible(false);
            rectMode = false;

            var bounds = selectRect.localToParent(selectRect.getBoundsInLocal());

            if (onSelected != null)
                onSelected.accept(bounds);
        });
        setOnMouseDragged(a -> {
            if (!a.getTarget().equals(this))
                return;

            if (!rectMode){
                rectStartX = a.getX();
                rectStartY = a.getY();
                selectRect.setWidth(0);
                selectRect.setHeight(0);
                selectRect.setVisible(true);
                //mainSelectionMenu.setVisible(false);
                if (onSelectBegin != null)
                    onSelectBegin.accept(new Point2D(rectStartX, rectStartY));
                rectMode = true;
            }
            else{
                selectRect.setX(Math.min(rectStartX, a.getX()));
                selectRect.setY(Math.min(rectStartY, a.getY()));
                selectRect.setWidth(Math.abs(rectStartX - a.getX()));
                selectRect.setHeight(Math.abs(rectStartY - a.getY()));
            }
        });

        navMaster.widthProperty().addListener(this::onWidthChanged);
        widthProperty().addListener(this::onWidthChanged);

        navMaster.visibleProperty().bind(nav.enabledProperty());

        selectedItems.addListener((ListChangeListener<T>) c -> {
            nav.setEnabled(!selectedItems.isEmpty());
            selectedLabel.setValue(selectedItems.size());
        });
    }

    private void onWidthChanged(Observable o){
        navMaster.setLayoutX(getWidth() - navMaster.getWidth() - 10);
        navMaster.setLayoutY(10);
    }

    public CNav getNav(){
        return nav;
    }

    public void cancelSelection(){
        clearSelection();
        if (onSelectCancel != null)
            onSelectCancel.run();
    }

    public void close(){
        rectStartX = rectStartY = 0;
        selectRect.setWidth(0);
        selectRect.setHeight(0);
        selectRect.setVisible(false);
        nav.setEnabled(false);
    }

    public void clearSelection(){
        close();
        selectedItems.clear();
    }

    public List<T> getSelectedItems(){
        return selectedItems;
    }

    public void setOnSelected(Consumer<Bounds> onSelected){
        this.onSelected = onSelected;
    }

    public void setOnSelectBegin(Consumer<Point2D> onSelectBegin){
        this.onSelectBegin = onSelectBegin;
    }
    public void setOnSelectCancelled(Runnable onSelectCancel){
        this.onSelectCancel = onSelectCancel;
    }

    public boolean isSelectionMenuOpen(){
        return nav.isEnabled();
    }

    public record GridCellBounds(double minX, double minY, double contentX, double contentY, double width, double height){ }

    public GridCellBounds getGridCellBounds(Region node, double pad, double size, double spacing) {
        final double rawCoordX = node.getLayoutX() - pad;
        final double rawCoordY = node.getLayoutY() - pad;

        final double spacedSize = size + spacing;
        final double halfSpacing = spacing / 2.0;
        final double sideSize = size + halfSpacing;

        final double minX;
        final double posX;
        final double width;
        final double lastCoordX = getWidth() - sideSize;
        if (rawCoordX <= sideSize) {
            minX = 0;
            posX = minX;
            width = sideSize;
        }
        else if (rawCoordX >= lastCoordX) {
            minX = lastCoordX;
            posX = minX;
            width = sideSize;
        }
        else {
            minX = sideSize + Math.floor((rawCoordX - sideSize) / spacedSize) * spacedSize;
            posX = minX + halfSpacing;
            width = spacedSize;
        }

        final double minY;
        final double posY;
        final double height;
        final double lastCoordY = getHeight() - sideSize;
        if (rawCoordY <= sideSize) {
            minY = 0;
            posY = minY;
            height = sideSize;
        }
        else if (rawCoordY >= lastCoordY) {
            minY = lastCoordY;
            posY = minY;
            height = sideSize;
        }
        else {
            minY = sideSize + Math.floor((rawCoordY - sideSize) / spacedSize) * spacedSize;
            posY = minY + halfSpacing;
            height = spacedSize;
        }

        return new GridCellBounds(pad + minX, pad + minY, pad + posX, pad + posY, width, height);
    }
}
