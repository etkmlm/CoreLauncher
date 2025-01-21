package com.laeben.corelauncher.ui.entity;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.entity.Frame;
import com.laeben.corelauncher.ui.util.EventFilterManager;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class LStage extends Stage {
    private Frame frame;

    private LScene scene;
    private String name;

    private boolean moving;
    private double xD;
    private double yD;

    private Cursor c, cFinal;

    private final EventFilterManager efManager;

    public LStage(){
        getIcons().addAll(getIconSet());

        efManager = new EventFilterManager();

        focusedProperty().addListener((a, b, c) -> {
            if (c)
                return;
            moving = false;
            cFinal = null;
            this.c = Cursor.DEFAULT;
            if (scene != null)
                scene.setCursor(this.c);
        });
        addRegisteredEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        addRegisteredEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        //setOnMouseDragged(this::onMouseDragged);
        addRegisteredEventFilter(MouseEvent.MOUSE_RELEASED, a -> {
            cFinal = null;
            moving = false;
        });
    }

    public static Image[] getIconSet(){
        return new Image[]{
            new Image(Objects.requireNonNull(CoreLauncherFX.class.getResourceAsStream("/com/laeben/corelauncher/logo16x16.png"))),
            new Image(Objects.requireNonNull(CoreLauncherFX.class.getResourceAsStream("/com/laeben/corelauncher/logo32x32.png"))),
            new Image(Objects.requireNonNull(CoreLauncherFX.class.getResourceAsStream("/com/laeben/corelauncher/logo64x64.png")))
        };
    }

    public <T extends Event> void addRegisteredEventFilter(EventType<T> type, EventHandler<T> handler){
        efManager.addEventFilter(EventFilter.window(this, type, handler));
    }

    public LStage setName(String name){
        this.name = name;
        return this;
    }

    public String getName(){
        return name;
    }

    public LStage setStyle(StageStyle style){
        initStyle(style);
        return this;
    }

    public LStage setFrame(Frame f){
        frame = f;
        return this;
    }

    public Frame getFrame(){
        return frame;
    }

    public LStage setStageScene(LScene s){
        if (this.scene != null)
            this.scene.setOnMousePressed(null);

        setScene(s.setStage(this));

        s.setOnMouseClicked(this::onMousePressed);

        scene = s;
        return this;
    }

    public LStage setStageTitle(String title){
        setTitle(title);
        if (frame != null)
            frame.setTitle(title);
        return this;
    }

    public LScene getLScene(){
        return scene;
    }

    public void onMousePressed(MouseEvent e){
        moving = true;
        xD = getX() - e.getScreenX();
        yD = getY() - e.getScreenY();
    }

    public void onMouseMoved(MouseEvent e){
        if (cFinal != null || scene == null)
            return;

        double minX = getX();
        double minY = getY();
        double maxX = minX + getWidth();
        double maxY = minY + getHeight();

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

        if (scene.getCursor() != c)
            scene.setCursor(c);
    }

    public void onMouseDragged(MouseEvent e){
        if (e.getButton() != MouseButton.PRIMARY)
            return;

        if (c == null || c == Cursor.DEFAULT){
            if (moving){
                setX(e.getScreenX() + xD);
                setY(e.getScreenY() + yD);
            }
            return;
        }

        if (cFinal == null)
            cFinal = c;

        double minX = getX();
        double minY = getY();
        double maxX = minX + getWidth();
        double maxY = minY + getHeight();

        double mX = e.getScreenX();
        double mY = e.getScreenY();

        if (cFinal == Cursor.E_RESIZE){
            setWidth(mX - minX);
        }
        else if (cFinal == Cursor.NE_RESIZE){
            setWidth(mX - minX);
            setHeight(maxY - mY);
            setY(mY);
        }
        else if (cFinal == Cursor.SE_RESIZE){
            setWidth(mX - minX);
            setHeight(mY - minY);
        }
        else if (cFinal == Cursor.W_RESIZE){
            setWidth(maxX - mX);
            setX(mX);
        }
        else if (cFinal == Cursor.N_RESIZE){
            setHeight(maxY - mY);
            setY(mY);
        }
        else if (cFinal == Cursor.S_RESIZE){
            setHeight(mY - minY);
        }
        else if (cFinal == Cursor.NW_RESIZE){
            setHeight(maxY - mY);
            setY(mY);
            setWidth(maxX - mX);
            setX(mX);
        }
        else if (cFinal == Cursor.SW_RESIZE){
            setHeight(mY - minY);
            setWidth(maxX - mX);
            setX(mX);
        }
    }

    public void dispose(){
        efManager.clear();

        if (this.scene != null)
            this.scene.setOnMousePressed(null);
    }
}
