package com.laeben.corelauncher.ui.dialog;

import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.dialog.entity.DialogResult;
import com.laeben.corelauncher.ui.entity.LStage;
import com.laeben.corelauncher.ui.util.ControlUtil;
import com.laeben.corelauncher.wrap.ExtensionWrapper;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.Optional;

public class CDialog<T> extends Dialog<T> {
    public static final String API_DIALOG_CREATE = "onDialogCreate";
    public static final String DIALOG_SHOWN = "dialogShown";
    public static final String DIALOG_HIDE = "dialogHide";

    protected Node node;

    private DialogResult<T> result = null;

    protected final TranslateTransition trns;
    private final boolean enableAnimation;

    public CDialog(String fxml, boolean enableAnimation){
        this(fxml, enableAnimation, null);
    }

    public CDialog(String fxml, boolean enableAnimation, Window owner){
        this.enableAnimation = enableAnimation;

        if (owner != null)
            initOwner(owner);

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

        final var window = (Stage)getDialogPane().getScene().getWindow();

        window.getIcons().addAll(LStage.getIconSet());

        setResultConverter(a -> null);

        setOnShown(x -> {
            if (Main.getMain() == null)
                return;
            var parentBounds = Main.getMain().getTabBounds();

            window.setX(parentBounds.getMinX() + (parentBounds.getWidth() - window.getWidth()) / 2);
            window.setY(parentBounds.getMinY() + (parentBounds.getHeight() - window.getHeight()) / 2);
        });

        window.addEventFilter(MouseEvent.MOUSE_PRESSED, a -> {
            if (a.getButton() == MouseButton.MIDDLE && Configurator.getConfig().isEnabledMiddlePaste()) {
                var tf = ControlUtil.getTextFieldParent(a.getTarget());
                if (tf != null && tf.isEditable()) tf.paste();
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
        if (Main.getMain() != null)
            Main.getMain().setDialogLayer(true);
        UI.getUI().getHandler().execute(new KeyEvent(DIALOG_SHOWN).setSource(this));
        if (enableAnimation){
            node.setTranslateY(1000);
            trns.playFromStart();
        }
        var res = super.showAndWait();
        if (Main.getMain() != null)
            Main.getMain().setDialogLayer(false);
        UI.getUI().getHandler().execute(new KeyEvent(DIALOG_HIDE).setSource(this));

        return res;
    }

    protected DialogResult<T> actionForResult(){
        var o = action();

        return result != null ? result : (o.isEmpty() ? new DialogResult.Cancelled<>() : new DialogResult.Completed<>(o.get()));
    }

    protected CMsgBox showMsg(Alert.AlertType type, String title, String desc){
        return CMsgBox.msg(type, title, desc, getOwner());
    }

    protected void setDialogResult(DialogResult<T> result){
        this.result = result;
    }
}
