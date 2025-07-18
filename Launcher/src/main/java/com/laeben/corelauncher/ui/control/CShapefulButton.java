package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.ui.entity.animation.BackgroundColorAnimation;
import com.laeben.corelauncher.ui.entity.animation.ColorAnimation;
import javafx.animation.FadeTransition;
import javafx.beans.property.*;
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

public class CShapefulButton extends HBox {

    private static final FontCssMetaData<CShapefulButton> FONT =
            new FontCssMetaData<>("-fx-font", Font.getDefault()) {

                @Override
                public boolean isSettable(CShapefulButton n) {
                    return n == null || !n.font.isBound();
                }

                @Override
                public StyleableProperty<Font> getStyleableProperty(CShapefulButton n) {
                    return n.font;
                }
            };

    private static final CssMetaData<CShapefulButton, Paint> TEXT_FILL =
            new CssMetaData<>("-fx-text-fill",
                    PaintConverter.getInstance(), Color.BLACK) {

                @Override
                public boolean isSettable(CShapefulButton n) {
                    return n.textFill == null || !n.textFill.isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(CShapefulButton n) {
                    return n.textFill;
                }
            };

    private static final CssMetaData<CShapefulButton, Shape> SHAPE =
            new CssMetaData<>("-shape",
                    ShapeConverter.getInstance(), null) {

                @Override
                public boolean isSettable(CShapefulButton n) {
                    return n.shape == null || !n.shape.isBound();
                }

                @Override
                public StyleableProperty<Shape> getStyleableProperty(CShapefulButton n) {
                    return n.shape;
                }
            };

    private static final CssMetaData<CShapefulButton, Number> SHAPE_WIDTH =
            new CssMetaData<>("-shape-width",
                    StyleConverter.getSizeConverter(), 16) {

                @Override
                public boolean isSettable(CShapefulButton n) {
                    return n.shapeWidth == null || !n.shapeWidth.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(CShapefulButton n) {
                    return n.shapeWidth;
                }
            };

    private static final CssMetaData<CShapefulButton, Number> SHAPE_HEIGHT =
            new CssMetaData<>("-shape-height",
                    StyleConverter.getSizeConverter(), 16) {

                @Override
                public boolean isSettable(CShapefulButton n) {
                    return n.shapeHeight == null || !n.shapeHeight.isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(CShapefulButton n) {
                    return n.shapeHeight;
                }
            };

    private static final CssMetaData<CShapefulButton, Paint> SHAPE_FILL =
            new CssMetaData<>("-shape-fill",
                    PaintConverter.getInstance(), Color.BLACK) {

                @Override
                public boolean isSettable(CShapefulButton n) {
                    return n.shapeFill == null || !n.shapeFill.isBound();
                }

                @Override
                public StyleableProperty<Paint> getStyleableProperty(CShapefulButton n) {
                    return n.shapeFill;
                }
            };

    private static final CssMetaData<CShapefulButton, Boolean> ENABLE_TRANSPARENT_ANIMATION =
            new CssMetaData<>("-enable-transparent-animation",
                    BooleanConverter.getInstance(), false) {

                @Override
                public boolean isSettable(CShapefulButton n) {
                    return n.enableTransparentAnimation == null || !n.enableTransparentAnimation.isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(CShapefulButton n) {
                    return n.enableTransparentAnimation;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

    static {
        var l1 = new ArrayList<>(HBox.getClassCssMetaData());
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

    private final StringProperty text;
    private final StyleableBooleanProperty enableTransparentAnimation;
    private final StyleableObjectProperty<Shape> shape;
    private final StyleableDoubleProperty shapeWidth;
    private final StyleableDoubleProperty shapeHeight;
    private final StyleableObjectProperty<Font> font;
    private final StyleableObjectProperty<Paint> textFill;
    private final StyleableObjectProperty<Paint> shapeFill;

    private final FadeTransition fade;
    private final FadeTransition fadeDeeper;
    private final ColorAnimation backgroundAnimation;

    private final Text textControl;
    private final Region leftShapeRect;

    private Tooltip tip;

    private boolean trns;

    public CShapefulButton(){
        getStyleClass().addAll("cbutton", "cshapeful-button");

        text = new SimpleStringProperty(null);
        enableTransparentAnimation = new SimpleStyleableBooleanProperty(ENABLE_TRANSPARENT_ANIMATION);
        shape = new SimpleStyleableObjectProperty<>(SHAPE);
        shapeWidth = new SimpleStyleableDoubleProperty(SHAPE_WIDTH);
        shapeHeight = new SimpleStyleableDoubleProperty(SHAPE_HEIGHT);
        font = new SimpleStyleableObjectProperty<>(FONT);
        textFill = new SimpleStyleableObjectProperty<>(TEXT_FILL);
        shapeFill = new SimpleStyleableObjectProperty<>(SHAPE_FILL);

        setHgrow(this, Priority.NEVER);

        shape.addListener((a, b, shape) -> {
            if (shape != null){
                if (shapeFill.isBound())
                    shape.setFill(shapeFill.get());
                shape.fillProperty().bind(shapeFill);
            }
        });

        setAlignment(Pos.CENTER);

        textControl = new Text();
        textControl.textProperty().bindBidirectional(text);
        textControl.fontProperty().bind(font);

        leftShapeRect = new Region();
        leftShapeRect.prefWidthProperty().bind(shapeWidth);
        shapeFill.addListener((a, b, fill) -> {
            var background = new Background(new BackgroundFill(fill, null, null));
            leftShapeRect.setBackground(background);
        });
        shapeHeight.addListener((a, b, height) -> {
            leftShapeRect.setPrefHeight(height.doubleValue());
            leftShapeRect.setMaxHeight(height.doubleValue());
        });
        leftShapeRect.getStyleClass().addAll("shape", "left-shape");

        textControl.fillProperty().bind(textFill);

        shape.addListener((a, b, c) -> {
            leftShapeRect.setShape(c);
            if (c == null){
                getChildren().remove(leftShapeRect);

            }
            else{
                getChildren().add(0, leftShapeRect);
            }
        });

        setSpacing(6);
        getChildren().addAll(textControl);

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
        backgroundAnimation.setNode(this);

        setOnMouseEntered((a) -> {
            if (trns = (getBackground() == null || (!getBackground().getFills().isEmpty() && !getBackground().getFills().get(0).getFill().isOpaque())) && getEnableTransparentAnimation()){
                backgroundAnimation.playFromStart();
            }
            else
                fade.playFromStart();
        });

        setOnMouseExited((a) -> {
            if (trns && getEnableTransparentAnimation()){
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

    public void setText(String text){
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

    public void setTooltip(String text){
        if (tip == null){
            Tooltip.install(textControl, tip = new Tooltip(text));
        }
        else
            tip.setText(text);
    }
}
