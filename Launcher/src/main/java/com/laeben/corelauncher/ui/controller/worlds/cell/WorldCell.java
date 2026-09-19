package com.laeben.corelauncher.ui.controller.worlds.cell;

import com.laeben.core.entity.Path;
import com.laeben.core.event.context.EventContext;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.concurrency.TaskRecord;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.controller.cell.CVirtualCell;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;

public class WorldCell extends CVirtualCell<WorldCellItem> {
    private final StackPane root;

    private final Label label;
    private final ProgressBar progress;
    private final CButton btnCancel;

    private final ChangeListener<TaskRecord> importTaskListener;

    private ObjectProperty<WorldCellItem> selectedWorld;

    public WorldCell() {
        super(new StackPane());
        root = (StackPane) getRoot();

        progress = new ProgressBar();
        progress.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(progress);

        btnCancel = new CButton();
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
        btnCancel.setText(Translator.translate("option.cancel"));
        btnCancel.setStyle("-fx-background-color: transparent");
        btnCancel.setOnMouseClicked(a -> getItem().importRecord().cancelIfRunning());

        label = new Label();
        setOnMouseEntered(a -> {
            if (getItem() == null) return;

            if (getItem().getWorld() != null && getItem().getWorld().dirName != null)
                label.setText(getItem().getWorld().dirName);
            else if (getItem().importRecord().isRunning()){
                btnCancel.setVisible(true);
                btnCancel.setManaged(true);
                label.setVisible(false);
            }
        });
        setOnMouseExited(a -> {
            if (btnCancel.isVisible()){
                btnCancel.setVisible(false);
                btnCancel.setManaged(false);
                label.setVisible(true);
            }
            if (getItem() != null && getItem().getWorld() != null)
                label.setText(getItem().getWorld().getIdentifier());
        });
        root.getChildren().addAll(label, btnCancel);

        importTaskListener = (ob, o, n) -> {
            if (n != null) return;
            if (getItem().getWorld() == null) UI.runAsync(this::destroyItem);
            else UI.runAsync(() -> bindItem(getItem()));
        };
    }

    public WorldCell setSelectionProperty(ObjectProperty<WorldCellItem> property){
        this.selectedWorld = property;
        return this;
    }

    @Override
    public void updateSelected(boolean value){
        if (!value) {
            super.updateSelected(false);
            return;
        }

        if (getItem() != null && getItem().getWorld() != null && selectedWorld != null) {
            super.updateSelected(true);
            selectedWorld.set(getItem());
        }
        else setFocused(false);
    }

    private Translator.Cache extractCache;
    private void onProgress(long current, long total, EventContext context){
        final String t = (extractCache = Translator.Cache.translate("worlds.import.extract", extractCache)).getTranslation();

        if (context instanceof Path.ParentItemContext ctx) progress.setProgress(current * 1.0 / total);

        label.setText(t + " " + context);
    }

    @Override
    public void unbindItem(WorldCellItem item){
        label.setText(null);
        progress.setProgress(0);
        item.setOnProgress(null);
        item.importRecord().removeListener(importTaskListener);
    }

    @Override
    public void bindItem(WorldCellItem item) {
        if (item == null) return;

        if (item.getWorld() != null){
            progress.setVisible(false);
            progress.setManaged(false);
            label.setText(item.getWorld().getIdentifier());
        }
        else { // do not rebind the same item which in importing progress
            progress.setVisible(true);
            progress.setManaged(true);
            item.setOnProgress(this::onProgress);
            item.importRecord().addListener(importTaskListener);
            if (item.getTimer() != null) item.getTimer().triggerLatestTick();
        }
    }
}
