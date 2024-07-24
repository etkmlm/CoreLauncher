package com.laeben.corelauncher.ui.entity;

import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class LScene<T> extends Scene {
    private final T controller;
    private LStage stage;

    private boolean moving;
    private double xD;
    private double yD;

    private Cursor c, cFinal;


    public LScene(Parent root, T controller) {
        super(root);

        this.controller = controller;

        setOnMousePressed(this::onMousePressed);
    }

    public void onMousePressed(MouseEvent e){
        moving = true;
        xD = stage.getX() - e.getScreenX();
        yD = stage.getY() - e.getScreenY();
    }

    public void onMouseMoved(MouseEvent e){
        if (cFinal != null)
            return;

        double minX = stage.getX();
        double minY = stage.getY();
        double maxX = minX + stage.getWidth();
        double maxY = minY + stage.getHeight();

        double mX = e.getScreenX();
        double mY = e.getScreenY();

        boolean east = mX >= maxX -5 && mX <= maxX + 5;
        boolean north = mY >= minY - 5 && mY <= minY + 5;
        boolean south = mY <= maxY + 5 && mY >= maxY - 5;
        boolean west = mX >= minX - 5 && mX <= minX + 5;

        c = Cursor.DEFAULT;

        if (east && !north && !south){
            c = Cursor.E_RESIZE;
        }
        if (east && north){
            c = Cursor.NE_RESIZE;
        }
        if (east && south){
            c = Cursor.SE_RESIZE;
        }
        if (west && !north && !south){
            c = Cursor.W_RESIZE;
        }
        if (west && south){
            c = Cursor.SW_RESIZE;
        }
        if (west && north){
            c = Cursor.NW_RESIZE;
        }
        if (north && !west && !east){
            c = Cursor.N_RESIZE;
        }
        if (south && !east && !west){
            c = Cursor.S_RESIZE;
        }

        if (getCursor() != c)
            setCursor(c);
    }

    public void onMouseDragged(MouseEvent e){
        if (e.getButton() != MouseButton.PRIMARY)
            return;

        if (c == null || c == Cursor.DEFAULT){
            if (moving){
                stage.setX(e.getScreenX() + xD);
                stage.setY(e.getScreenY() + yD);
            }
            return;
        }

        if (cFinal == null)
            cFinal = c;

        double minX = stage.getX();
        double minY = stage.getY();
        double maxX = minX + stage.getWidth();
        double maxY = minY + stage.getHeight();

        double mX = e.getScreenX();
        double mY = e.getScreenY();

        if (cFinal == Cursor.E_RESIZE){
            stage.setWidth(mX - minX);
        }
        else if (cFinal == Cursor.NE_RESIZE){
            stage.setWidth(mX - minX);
            stage.setHeight(maxY - mY);
            stage.setY(mY);
        }
        else if (cFinal == Cursor.SE_RESIZE){
            stage.setWidth(mX - minX);
            stage.setHeight(mY - minY);
        }
        else if (cFinal == Cursor.W_RESIZE){
            stage.setWidth(maxX - mX);
            stage.setX(mX);
        }
        else if (cFinal == Cursor.N_RESIZE){
            stage.setHeight(maxY - mY);
            stage.setY(mY);
        }
        else if (cFinal == Cursor.S_RESIZE){
            stage.setHeight(mY - minY);
        }
        else if (cFinal == Cursor.NW_RESIZE){
            stage.setHeight(maxY - mY);
            stage.setY(mY);
            stage.setWidth(maxX - mX);
            stage.setX(mX);
        }
        else if (cFinal == Cursor.SW_RESIZE){
            stage.setHeight(mY - minY);
            stage.setWidth(maxX - mX);
            stage.setX(mX);
        }
    }


    public LScene setStage(LStage stage){
        this.stage = stage;

        stage.addEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        stage.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        //setOnMouseDragged(this::onMouseDragged);
        stage.addEventFilter(MouseEvent.MOUSE_RELEASED, a -> {
            cFinal = null;
            moving = false;
        });

        return this;
    }
    public T getController(){
        return controller;
    }
}
