package com.laeben.corelauncher.util.entity;

import javafx.concurrent.Task;
import javafx.scene.image.Image;

import java.util.function.Function;

public abstract class ImageTask extends Task<Image> {
    public static final ImageTask NULL = new ImageTask() {
        @Override
        protected Image evaluate() {
            return null;
        }
    };

    private final double width, height;

    private Function<Image, Image> _then;

    public ImageTask(){
        this(-1, -1);
    }

    public static ImageTask fromImage(Image image, double width, double height){
        return new ImageTask(width, height) {
            @Override
            protected Image evaluate() throws Exception {
                return image;
            }
        };
    }

    public static ImageTask fromImage(Image image){
        double width = image == null ? -1 : image.getWidth();
        double height = image == null ? -1 : image.getHeight();
        return new ImageTask(width, height) {
            @Override
            protected Image evaluate() throws Exception {
                return image;
            }
        };
    }

    public ImageTask(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double getRequestedWidth() {
        return width;
    }

    public double getRequestedHeight() {
        return height;
    }

    protected abstract Image evaluate() throws Exception;

    public ImageTask then(Function<Image, Image> then){
        this._then = then;
        return this;
    }

    @Override
    protected final Image call() throws Exception {
        var eval = evaluate();
        return _then != null ? _then.apply(eval) : eval;
    }

}
