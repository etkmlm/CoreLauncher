package com.laeben.corelauncher.ui.control;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class CColorPicker extends VBox {
    private static final CornerRadii DEFAULT_RADIUS = new CornerRadii(16);
    private static final Insets DEFAULT_INSETS = new Insets(16);

    private final int indicatorPadding = 8;

    private final SimpleBooleanProperty selectedColorVisible;

    private final SimpleDoubleProperty hue;
    private final SimpleDoubleProperty indicatorSize;
    private final SimpleDoubleProperty alpha;
    private final ObjectProperty<Color> currentColor;
    private final ObjectProperty<Color> selectedColor;

    private boolean changedAlphaManually = false;

    public CColorPicker() {
        hue = new SimpleDoubleProperty(this, "hue", 0);
        indicatorSize = new SimpleDoubleProperty(this, "indicatorSize", 64);
        alpha = new SimpleDoubleProperty(this, "opacity", 1.0);
        currentColor = new SimpleObjectProperty<>(this, "currentColor");
        selectedColor = new SimpleObjectProperty<>(this, "selectedColor");
        selectedColorVisible = new SimpleBooleanProperty(this, "selectedColorVisible", false);

        alpha.addListener((o, ov, nv) -> {
            var c = getSelectedColor();
            if (nv == null || c == null) return;
            changedAlphaManually = true;
            setSelectedColor(Color.color(c.getRed(), c.getGreen(), c.getBlue(), nv.doubleValue()));
            changedAlphaManually = false;
        });
        selectedColor.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            if (hue.get() != newValue.getHue())
               hue.set(newValue.getHue());

            if (getAlpha() != newValue.getOpacity() && !changedAlphaManually)
               setAlpha(newValue.getOpacity());
        });

        var pickerRow = new HBox();
        VBox.setVgrow(pickerRow, Priority.ALWAYS);
        pickerRow.setSpacing(8);

        var picker = generateColorPicker();
        HBox.setHgrow(picker, Priority.ALWAYS);
        var hueSelector = generateHuePicker();

        pickerRow.getChildren().setAll(picker, hueSelector);

        getChildren().add(pickerRow);

        var rect = new Pane();
        rect.setPrefWidth(50);
        rect.setPrefHeight(50);
        rect.backgroundProperty().bind(new ObjectBinding<>() {{
            bind(selectedColor);
        }
            @Override
            protected Background computeValue() {
                return new Background(new BackgroundFill(
                        selectedColor.get(),
                        CornerRadii.EMPTY, Insets.EMPTY));
            }
        });
        rect.managedProperty().bind(selectedColorVisible);
        rect.visibleProperty().bind(selectedColorVisible);

        getChildren().add(rect);
    }

    public SimpleDoubleProperty alphaProperty() {
        return alpha;
    }

    public double getAlpha(){
        return alpha.get();
    }

    public void setAlpha(double alpha){
        this.alpha.set(alpha);
    }

    public SimpleBooleanProperty selectedColorVisibleProperty() {
        return selectedColorVisible;
    }

    public void setSelectedColorVisible(boolean selectedColorVisible) {
        this.selectedColorVisible.set(selectedColorVisible);
    }

    public boolean isSelectedColorVisible() {
        return selectedColorVisible.get();
    }

    public ObjectProperty<Color> selectedColorProperty() {
        return selectedColor;
    }

    public Color getSelectedColor() {
        return selectedColor.get();
    }

    public void setSelectedColor(Color color) {
        selectedColor.set(color);
    }

    public ObjectProperty<Color> currentColorProperty() {
        return currentColor;
    }

    public Color getCurrentColor() {
        return currentColor.get();
    }

    public double getIndicatorSize() {
        return indicatorSize.get();
    }

    public void setIndicatorSize(double size) {
        indicatorSize.set(size);
    }

    private StackPane generateColorPicker(){
        var picker = new StackPane();

        var pane0 = new Pane();
        pane0.setMaxHeight(Double.MAX_VALUE);
        pane0.setMaxWidth(Double.MAX_VALUE);
        pane0.backgroundProperty().bind(new ObjectBinding<>() {{
            bind(hue);
        }
            @Override
            protected Background computeValue() {
                return new Background(new BackgroundFill(
                        Color.hsb(hue.get(), 1.0, 1.0),
                        CornerRadii.EMPTY, Insets.EMPTY));
            }
        });

        var pane1 = new Pane();
        pane1.setMaxHeight(Double.MAX_VALUE);
        pane1.setMaxWidth(Double.MAX_VALUE);
        pane1.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 255, 255, 1)),
                        new Stop(1, Color.rgb(255, 255, 255, 0))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        var pane2 = new Pane();
        pane2.setMaxHeight(Double.MAX_VALUE);
        pane2.setMaxWidth(Double.MAX_VALUE);
        pane2.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(0, 0, 0, 0)), new Stop(1, Color.rgb(0, 0, 0, 1))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        var p = new Pane();
        var indicator = generateIndicator();
        indicator.setVisible(false);
        p.getChildren().add(indicator);

        picker.getChildren().setAll(pane0, pane1, pane2, p);

        EventHandler<MouseEvent> handler = (MouseEvent event) -> {
            double xp = event.getX() / picker.getWidth();
            double yp = event.getY() / picker.getHeight();

            if (xp > 1 || yp > 1 || xp < 0 || yp < 0) {
                indicator.setVisible(false);
                return;
            }
            else if (!indicator.isVisible())
                indicator.setVisible(true);

            indicator.setLayoutX(event.getX() + (event.getX() + getIndicatorSize() + indicatorPadding > picker.getWidth() ? -getIndicatorSize() - indicatorPadding : indicatorPadding));
            indicator.setLayoutY(event.getY() + (event.getY() + getIndicatorSize() + indicatorPadding > picker.getHeight() ? -getIndicatorSize() - indicatorPadding : indicatorPadding));
            currentColor.set(Color.hsb(
                    hue.get(), xp, 1 - yp, getAlpha()
            ));

            if (event.getEventType() == MouseEvent.MOUSE_DRAGGED)
                setSelectedColor(getCurrentColor());
        };

        picker.setOnMouseDragged(handler);
        picker.setOnMouseMoved(handler);
        picker.setOnMouseExited(event -> indicator.setVisible(false));
        picker.setOnMouseClicked(event -> setSelectedColor(getCurrentColor()));

        return picker;
    }

    private Pane generateIndicator(){
        final double size = getIndicatorSize();

        var rr = new Pane();
        rr.setBackground(null);
        rr.setMaxHeight(size);
        rr.setMaxWidth(size);
        rr.setPrefWidth(size);
        rr.setPrefHeight(size);
        rr.backgroundProperty().bind(new ObjectBinding<>() {{
            bind(currentColor);
        }
            @Override
            protected Background computeValue() {
                return new Background(new BackgroundFill(getCurrentColor(), DEFAULT_RADIUS, null));
            }
        });
        return rr;
    }

    private Pane generateHuePicker(){
        var hueSelector = new Pane();
        hueSelector.setPrefWidth(30);
        hueSelector.setPrefHeight(30);
        hueSelector.setMaxHeight(Double.MAX_VALUE);
        hueSelector.setBackground(new Background(new BackgroundFill(
                createHueGradient(), CornerRadii.EMPTY, Insets.EMPTY)));

        EventHandler<MouseEvent> clicked =  (MouseEvent e) ->
                hue.set(e.getY() / hueSelector.getHeight() * 360);

        hueSelector.setOnMouseClicked(clicked);
        hueSelector.setOnMouseDragged(clicked);

        return hueSelector;
    }

    private static LinearGradient createHueGradient() {
        double offset;
        Stop[] stops = new Stop[255];
        for (int y = 0; y < 255; y++) {
            offset = 1 - (1.0 / 255) * y;
            int h = (int) ((y / 255.0) * 360);
            stops[y] = new Stop(offset, Color.hsb(h, 1.0, 1.0));
        }
        return new LinearGradient(0f, 1f, 0f, 0f, true, CycleMethod.NO_CYCLE, stops);
    }
}
