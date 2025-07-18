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
import java.util.function.Consumer;

public class LStage extends Stage {
    private static final double PADDING_RESIZE = 5;
    private static final double PADDING_MOVE_TOP = 30;
    private static final double PADDING_MOVE_RIGHT = 15;
    private static final double PADDING_MOVE_BOTTOM = 15;
    private static final double PADDING_MOVE_LEFT = 15;

    public record WindowSizeChangedEventArgs(double width, double height, boolean oldMax, boolean newMax) {

    }


    private Frame frame;

    private LScene scene;
    private String name;

    private boolean moving;
    private boolean ownDragging;
    private boolean dragging;
    private double mouseRelativeX;
    private double mouseRelativeY;

    private Cursor c, cFinal;

    private Consumer<WindowSizeChangedEventArgs> onWindowSizeChanged;

    private final EventFilterManager efManager;

    public LStage(){
        getIcons().addAll(getIconSet());

        efManager = new EventFilterManager();

        focusedProperty().addListener((a, b, c) -> {
            if (c)
                return;
            moving = false;
            dragging = false;
            ownDragging = false;
            cFinal = null;
            this.c = Cursor.DEFAULT;
            if (scene != null)
                scene.setCursor(this.c);
        });
        addRegisteredEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        addRegisteredEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        addRegisteredEventFilter(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
        maximizedProperty().addListener((a, b, c) -> {
            if (onWindowSizeChanged != null)
                onWindowSizeChanged.accept(new WindowSizeChangedEventArgs(getWidth(), getHeight(), b, c));
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

    public void setOnWindowSizeChanged(Consumer<WindowSizeChangedEventArgs> onWindowSizeChanged){
        this.onWindowSizeChanged = onWindowSizeChanged;
    }

    public void onMouseReleased(MouseEvent e){
        if (cFinal != null && onWindowSizeChanged != null) {
            onWindowSizeChanged.accept(new WindowSizeChangedEventArgs(getWidth(), getHeight(), isMaximized(), isMaximized()));
        }

        cFinal = null;
        moving = false;
        dragging = false;
        ownDragging = false;
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

        boolean east = mX >= maxX - PADDING_RESIZE && mX <= maxX + PADDING_RESIZE;
        boolean north = mY >= minY - PADDING_RESIZE && mY <= minY + PADDING_RESIZE;
        boolean south = mY <= maxY + PADDING_RESIZE && mY >= maxY - PADDING_RESIZE;
        boolean west = mX >= minX - PADDING_RESIZE && mX <= minX + PADDING_RESIZE;

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

            if (!dragging){
                dragging = true;

                boolean isInBorders =
                        e.getScreenX() < getX() + PADDING_MOVE_LEFT ||
                        e.getScreenX() > getX() + getWidth() - PADDING_MOVE_RIGHT ||
                        e.getScreenY() < getY() + PADDING_MOVE_TOP ||
                        e.getScreenY() > getY() + getHeight() - PADDING_MOVE_BOTTOM;

                if (!isInBorders){
                    ownDragging = false;
                    return;
                }
                ownDragging = true;
            }
            else if (!ownDragging)
                return;

            if (moving){
                setX(e.getScreenX() - mouseRelativeX);
                setY(e.getScreenY() - mouseRelativeY);
            }
            else {
                mouseRelativeX = e.getScreenX() - getX();
                mouseRelativeY = e.getScreenY() - getY();
                moving = true;
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

        double w, h;

        if (cFinal == Cursor.E_RESIZE){
            w = mX - minX;
            if (w < getMinWidth()){
                onMouseReleased(e);
                return;
            }
            setWidth(w);
        }
        else if (cFinal == Cursor.NE_RESIZE){
            w = mX - minX;
            h = maxY - mY;

            if (h < getMinHeight() || w < getMinWidth()){
                onMouseReleased(e);
                return;
            }

            setWidth(w);
            setHeight(h);
            setY(mY);
        }
        else if (cFinal == Cursor.SE_RESIZE){
            w = mX - minX;
            h = mY - minY;
            if (h < getMinHeight() || w < getMinWidth()){
                onMouseReleased(e);
                return;
            }

            setWidth(w);
            setHeight(h);
        }
        else if (cFinal == Cursor.W_RESIZE){
            w = maxX - mX;
            if (w < getMinWidth()){
                onMouseReleased(e);
                return;
            }
            setWidth(w);
            setX(mX);
        }
        else if (cFinal == Cursor.N_RESIZE){
            h = maxY - mY;
            if (h < getMinHeight()){
                onMouseReleased(e);
                return;
            }
            setHeight(h);
            setY(mY);
        }
        else if (cFinal == Cursor.S_RESIZE){
            h = mY - minY;
            if (h < getMinHeight()){
                onMouseReleased(e);
                return;
            }
            setHeight(h);
        }
        else if (cFinal == Cursor.NW_RESIZE){
            w = maxX - mX;
            h = maxY - mY;
            if (h < getMinHeight() || w < getMinWidth()){
                onMouseReleased(e);
                return;
            }
            setHeight(h);
            setY(mY);
            setWidth(w);
            setX(mX);
        }
        else if (cFinal == Cursor.SW_RESIZE){
            w = maxX - mX;
            h = mY - minY;
            if (h < getMinHeight() || w < getMinWidth()){
                onMouseReleased(e);
                return;
            }
            setHeight(h);
            setWidth(w);
            setX(mX);
        }
    }

    public void dispose(){
        efManager.clear();

        if (this.scene != null)
            this.scene.setOnMousePressed(null);
    }
}
