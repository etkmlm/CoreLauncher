package com.laeben.corelauncher.ui.dialog;

import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.entity.LStage;
import com.laeben.corelauncher.wrap.ExtensionWrapper;
import javafx.animation.TranslateTransition;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.util.Optional;

public class CDialog<T> extends Dialog<T> {
    public static final String API_DIALOG_CREATE = "onDialogCreate";
    public static final String DIALOG_SHOWN = "dialogShown";
    public static final String DIALOG_HIDE = "dialogHide";

    protected Node node;

    protected final TranslateTransition trns;
    private final boolean enableAnimation;

    public CDialog(String fxml, boolean enableAnimation){
        this.enableAnimation = enableAnimation;

        setGraphic(node = UI.getUI().load(CoreLauncherFX.class.getResource(fxml), this));
        initStyle(StageStyle.TRANSPARENT);
        //getDialogPane().setStyle("-fx-background-color: transparent");
        getDialogPane().getScene().setFill(null);
        getDialogPane().getStyleClass().add("cdialog");
        getDialogPane().getStylesheets().add(CoreLauncherFX.CLUI_CSS);
        getDialogPane().setPadding(Insets.EMPTY);
        var n = (Region)node;
        getDialogPane().prefHeightProperty().bind(n.prefHeightProperty());
        getDialogPane().prefWidthProperty().bind(n.prefWidthProperty());

        ((Stage)getDialogPane().getScene().getWindow()).getIcons().addAll(LStage.getIconSet());

        setResultConverter(a -> null);

        getDialogPane().getScene().getWindow().addEventFilter(EventType.ROOT, x -> {
            if (x instanceof WindowEvent we && we.getEventType() == WindowEvent.WINDOW_SHOWN){
                if (Main.getMain() == null)
                    return;
                var parentBounds = Main.getMain().getTabBounds();

                var window = (Window)we.getSource();

                window.setX(parentBounds.getMinX() + (parentBounds.getWidth() - window.getWidth()) / 2);
                window.setY(parentBounds.getMinY() + (parentBounds.getHeight() - window.getHeight()) / 2);
            }
        });

        trns = new TranslateTransition();
        trns.setNode(node);
        trns.setFromY(1000);
        trns.setToY(0);
        trns.setDuration(Duration.millis(500));

        ExtensionWrapper.getWrapper().fireEvent(API_DIALOG_CREATE, this, CDialog.class);
    }

    protected Optional<T> action(){
        Main.getMain().setDialogLayer(true);
        UI.getUI().getHandler().execute(new KeyEvent(DIALOG_SHOWN).setSource(this));
        if (enableAnimation){
            node.setTranslateY(1000);
            trns.playFromStart();
        }
        var res = super.showAndWait();
        Main.getMain().setDialogLayer(false);
        UI.getUI().getHandler().execute(new KeyEvent(DIALOG_HIDE).setSource(this));

        return res;
    }
}
