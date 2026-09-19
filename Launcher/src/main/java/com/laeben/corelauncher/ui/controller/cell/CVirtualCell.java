package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.UI;
import javafx.animation.FadeTransition;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.util.Duration;

public abstract class CVirtualCell<T> extends ListCell<T> {
    private final Node root;
    private final FadeTransition fade;

    protected CVirtualCell(String fxml) {
        this(null, fxml);
    }

    protected CVirtualCell(Node root) {
        this(root, null);
    }

    private CVirtualCell(Node root, String fxml) {
        assert root != null || fxml != null;
        this.root = root == null ? UI.getUI().load(CoreLauncherFX.class.getResource(fxml), this) : root;

        fade = new FadeTransition();
        fade.setFromValue(1);
        fade.setToValue(0.7);
        fade.setDuration(Duration.millis(200));
        fade.setNode(this.root);

        // important for fitting the cell to width
        setPrefWidth(0);

        setGraphic(this.root);
    }

    public void unbindItem(T item) {

    }

    public void emptyItem(){
        root.setVisible(false);
    }

    public void filledItem(){
        root.setVisible(true);
    }

    public abstract void bindItem(T item);

    @Override
    public final void updateItem(T item, boolean empty) {
        if (getItem() != null) unbindItem(getItem());

        super.updateItem(item, empty);

        if (item == null || empty) {
            emptyItem();
            return;
        }

        bindItem(item);
        filledItem();
    }

    public void rebindItem() {
        final var item = getItem();
        updateItem(item, isEmpty());
    }

    public void destroyItem() {
        final var item = getItem();
        unbindItem(item);

        if (getListView().getItems() instanceof FilteredList<T> lst) lst.getSource().remove(item);
        else getListView().getItems().remove(item);
    }

    public Node getRoot() {
        return root;
    }
}
