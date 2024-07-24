package com.laeben.corelauncher.ui.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.HBox;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public class CPaginator extends HBox {

    private Consumer<CPaginator> onPageChange;
    private final IntegerProperty page;
    private final IntegerProperty totalPages;

    private final CButton btnPrevious;
    private final CButton btnNext;
    private final CCombo<Integer> cbPages;

    public CPaginator() {
        totalPages = new SimpleIntegerProperty(0);
        page = new SimpleIntegerProperty(0);

        btnPrevious = new CButton();
        btnNext = new CButton();
        cbPages = new CCombo<>();
        cbPages.getItems().setAll(0);
        cbPages.setValue(0);

        getStyleClass().add("cpaginator");
        getChildren().addAll(btnPrevious, cbPages, btnNext);

        cbPages.setPrefWidth(60);
        //int i = cbPages.getValue();
        cbPages.setOnItemChanged(page::set);

        totalPages.addListener(a -> {
            var arr = new int[getTotalPages()];
            Arrays.setAll(arr, IntUnaryOperator.identity());
            cbPages.getItems().setAll(Arrays.stream(arr).boxed().map(x -> x + 1).toList());

            if (getTotalPages() > 0)
                cbPages.setValue(1);
        });

        page.addListener(a -> {
            if (onPageChange != null)
                onPageChange.accept(this);
        });

        btnPrevious.enableTransparentAnimation();
        btnPrevious.setText("<");
        btnPrevious.setOnMouseClicked(a -> {
            int val = getPage() - 1;
            if (val <= 0)
                return;
            cbPages.setValue(val);
        });

        btnNext.enableTransparentAnimation();
        btnNext.setText(">");
        btnNext.setOnMouseClicked(a -> {
            int val = getPage() + 1;
            if (val > getTotalPages())
                return;
            cbPages.setValue(val);
        });
    }

    public void setTotalPages(int totalPages){
        this.totalPages.set(totalPages);
    }

    public void setOnPageChange(final Consumer<CPaginator> onPageChange) {
        this.onPageChange = onPageChange;
    }

    public int getPage(){
        return page.get();
    }

    public int getTotalPages(){
        return totalPages.get();
    }
}
