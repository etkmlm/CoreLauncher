package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.ui.animation.color.BackgroundColorAnimation;
import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.*;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.ShapeConverter;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CButton extends StackPane {
    private static final FontCssMetaData<CButton> FONT =
            new FontCssMetaData<>("-fx-font", Font.getDefault()) {

                @Override
                public boolean isSettable(CButton n) {
                    return n == null || !n.font.isBound();
                }

                @Override
                public StyleableProperty<Font> getStyleableProperty(CButton n) {
                    return n.font;
                }
            };

    private static final CssMetaData<CButton, Paint> TEXT_FILL =
            new CssMetaData<>("-fx-text-fill",
                    PaintConverter.getInstance(), Color.BLACK) {

                @Override
                public boolean isSettable(CButton n) {
                    return n.textFill == null || !n.textFill.isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(CButton n) {
                    return n.textFill;
                }
            };

    private static final CssMetaData<CButton, Shape> SHAPE =
            new CssMetaData<>("-shape",
                    ShapeConverter.getInstance(), null) {

                @Override
                public boolean isSettable(CButton n) {
                    return n.shape == null || !n.shape.isBound();
                }

                @Override
                public StyleableProperty<Shape> getStyleableProperty(CButton n) {
                    return n.shape;
                }
            };

    private static final CssMetaData<CButton, Number> SHAPE_WIDTH =
            new CssMetaData<>("-shape-width",
                    StyleConverter.getSizeConverter(), 16) {

                @Override
                public boolean isSettable(CButton n) {
                    return n.shapeWidth == null || !n.shapeWidth.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(CButton n) {
                    return n.shapeWidth;
                }
            };

    private static final CssMetaData<CButton, Number> SHAPE_HEIGHT =
            new CssMetaData<>("-shape-height",
                    StyleConverter.getSizeConverter(), 16) {

                @Override
                public boolean isSettable(CButton n) {
                    return n.shapeHeight == null || !n.shapeHeight.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(CButton n) {
                    return n.shapeHeight;
                }
            };

    private static final CssMetaData<CButton, Paint> SHAPE_FILL =
            new CssMetaData<>("-shape-fill",
                    PaintConverter.getInstance(), Color.BLACK) {

                @Override
                public boolean isSettable(CButton n) {
                    return n.shapeFill == null || !n.shapeFill.isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(CButton n) {
                    return n.shapeFill;
                }
            };

    private static final CssMetaData<CButton, Boolean> ENABLE_TRANSPARENT_ANIMATION =
            new CssMetaData<>("-enable-transparent-animation",
                    BooleanConverter.getInstance(), false) {

                @Override
                public boolean isSettable(CButton n) {
                    return n.enableTransparentAnimation == null || !n.enableTransparentAnimation.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(CButton n) {
                    return n.enableTransparentAnimation;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

    static {
        var l1 = new ArrayList<>(StackPane.getClassCssMetaData());
        l1.add(FONT);
        l1.add(TEXT_FILL);
        l1.add(SHAPE_FILL);
        l1.add(ENABLE_TRANSPARENT_ANIMATION);
        l1.add(SHAPE);
        l1.add(SHAPE_WIDTH);
        l1.add(SHAPE_HEIGHT);
        cssMetaDataList = Collections.unmodifiableList(l1);
    }

    public static List <CssMetaData <? extends Styleable, ? > > getClassCssMetaData() {
        return cssMetaDataList;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    private final StyleableBooleanProperty enableTransparentAnimation;
    private final StyleableObjectProperty<Shape> shape;
    private final StyleableDoubleProperty shapeWidth;
    private final StyleableDoubleProperty shapeHeight;
    private final StyleableObjectProperty<Font> font;
    private final StyleableObjectProperty<Paint> textFill;
    private final StyleableObjectProperty<Paint> shapeFill;

    private final BooleanProperty useTransparentAnimation;
    private final StringProperty text;
    private final StringProperty tooltip;

    private final Region background;
    private final HBox container;
    private final Text label;
    private final Region leftShapeRect;

    private final FadeTransition fade;
    private final FadeTransition fadeDeeper;
    private final BackgroundColorAnimation backgroundAnimation;

    public void setTooltip(String text){
        this.tooltip.set(text);
    }
    public String getTooltip(){
        return this.tooltip.get();
    }

    public CButton(){
        getStyleClass().addAll("cbutton");
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        enableTransparentAnimation = new SimpleStyleableBooleanProperty(ENABLE_TRANSPARENT_ANIMATION);
        shape = new SimpleStyleableObjectProperty<>(SHAPE);
        shapeWidth = new SimpleStyleableDoubleProperty(SHAPE_WIDTH);
        shapeHeight = new SimpleStyleableDoubleProperty(SHAPE_HEIGHT);
        font = new SimpleStyleableObjectProperty<>(FONT);
        textFill = new SimpleStyleableObjectProperty<>(TEXT_FILL);
        shapeFill = new SimpleStyleableObjectProperty<>(SHAPE_FILL);

        useTransparentAnimation = new SimpleBooleanProperty();
        text = new SimpleStringProperty();
        tooltip = new SimpleStringProperty();
        tooltip.addListener((ob, o, n) -> Tooltip.install(this, n == null ? null : new Tooltip(n)));

        background = new Region();
        getChildren().add(background);

        container = new HBox();
        container.setSpacing(6);
        container.setAlignment(Pos.CENTER);
        getChildren().add(container);

        leftShapeRect = new Region();
        leftShapeRect.prefWidthProperty().bind(shapeWidth);

        label = new Text();
        label.fontProperty().bind(font);
        label.textProperty().bind(text);
        label.fillProperty().bind(textFill);

        shape.addListener((a, b, shape) -> {
            if (shape != null){
                if (shapeFill.isBound())
                    shape.setFill(shapeFill.get());
                shape.fillProperty().bind(shapeFill);

                if (b == null) container.getChildren().add(0, leftShapeRect);
            }
            else container.getChildren().remove(leftShapeRect);

            leftShapeRect.setShape(shape);
        });

        shapeFill.addListener((a, b, fill) -> {
            var background = new Background(new BackgroundFill(fill, null, null));
            leftShapeRect.setBackground(background);
        });
        shapeHeight.addListener((a, b, height) -> {
            leftShapeRect.setPrefHeight(height.doubleValue());
            leftShapeRect.setMaxHeight(height.doubleValue());
        });
        leftShapeRect.getStyleClass().addAll("shape", "left-shape");

        fade = new FadeTransition();
        fadeDeeper = new FadeTransition();
        fade.setNode(this);
        fadeDeeper.setNode(this);
        fade.setToValue(0.7);
        fade.setFromValue(1);
        fadeDeeper.setFromValue(1);
        fadeDeeper.setToValue(0.5);
        fade.setDuration(new Duration(200));
        fadeDeeper.setDuration(new Duration(300));

        backgroundAnimation = new BackgroundColorAnimation();
        backgroundAnimation.setColor(Color.rgb(2, 2, 2));
        backgroundAnimation.setDuration(Duration.millis(200));
        backgroundAnimation.setRegion(background);
        backgroundProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.getFills().isEmpty()) backgroundAnimation.setRadii(newValue.getFills().get(0).getRadii());
        });

        setOnMouseEntered((a) -> {
            if (getEnableTransparentAnimation())
                backgroundAnimation.playFromStart();
            else
                fade.playFromStart();
        });

        setOnMouseExited((a) -> {
            if (getEnableTransparentAnimation()){
                backgroundAnimation.play();
                backgroundAnimation.jumpTo(Duration.ZERO);
                backgroundAnimation.stop();
            }
            else {
                fade.play();
                fade.jumpTo(Duration.ZERO);
                fade.stop();
            }
        });

        setOnMousePressed((a) -> fadeDeeper.playFromStart());

        setOnMouseReleased((a) -> {
            fadeDeeper.play();
            fadeDeeper.jumpTo(Duration.ZERO);
            fadeDeeper.stop();
        });
    }

    @Override
    public void layoutChildren(){
        super.layoutChildren();

        background.resizeRelocate(0, 0, getWidth(), getHeight());
    }

    public void setText(String text){
        if (text == null){
            container.getChildren().remove(label);
        }
        else if (getText() == null){
            container.getChildren().add(0, label);
        }
        this.text.set(text);
    }

    public String getText(){
        return text.get();
    }

    public void setButtonShape(Shape shape){
        this.shape.set(shape);
    }

    public Shape getButtonShape(){
        return shape.get();
    }

    public void setFont(Font font){
        this.font.set(font);
    }

    public Font getFont(){
        return font.get();
    }

    public void setTextFill(Paint fill){
        this.textFill.set(fill);
    }

    public Paint getTextFill(){
        return shapeFill.get() == null ? Color.BLACK : shapeFill.get();
    }

    public void setEnableTransparentAnimation(boolean value){
        this.enableTransparentAnimation.set(value);
    }

    public boolean getEnableTransparentAnimation(){
        return this.enableTransparentAnimation.get();
    }

    public void enableTransparentAnimation(){
        setEnableTransparentAnimation(true);
    }
}
