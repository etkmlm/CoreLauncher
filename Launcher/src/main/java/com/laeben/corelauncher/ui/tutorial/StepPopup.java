package com.laeben.corelauncher.ui.tutorial;

import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CPopup;
import com.laeben.corelauncher.ui.control.CTab;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.dialog.CDialog;
import com.laeben.corelauncher.ui.tutorial.entity.Step;
import javafx.animation.ScaleTransition;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Consumer;

public class StepPopup extends Region {
    public static final String KEY = "popStep";


    public enum ConfirmState{
        PREVIOUS, NEXT, CANCEL
    }

    private Step step;
    private Consumer<ConfirmState> onConfirm;

    private final EventHandler<javafx.scene.input.KeyEvent> keyEventFilter;
    private final EventHandler<MouseEvent> mouseEventFilter;

    private final Node node1;
    private final Node node2;

    private final CPopup popup;
    private final ScaleTransition transition;

    public StepPopup() {
        getChildren().add(node1 = UI.getUI().load(CoreLauncherFX.class.getResource("layout/controls/steppopup.fxml"), this));
        setManaged(false);
        setVisible(false);

        transition = new ScaleTransition();
        transition.setFromX(0);
        transition.setToX(1);
        transition.setNode(this);
        transition.setDuration(Duration.millis(300));

        popup = new CPopup();
        popup.setDuration(300);
        popup.setAutoHide(false);
        var p = new Pane();
        p.getChildren().add(node2 = UI.getUI().load(CoreLauncherFX.class.getResource("layout/controls/steppopup.fxml"), this));
        popup.setContent(p);

        keyEventFilter = this::keyEventFilter;
        mouseEventFilter = this::mouseEventFilter;

        Main.getMain().getHandler().addHandler(KEY, a -> {
            if (step == null || step.getToController() == null || !a.getKey().equals(Main.TAB_FOCUS_CHANGE))
                return;

            var newTab = (CTab)a.getSource();
            if (newTab.getController().getClass().equals(step.getToController())){
                confirm(ConfirmState.NEXT);
            }
        }, true);
        UI.getUI().getHandler().addHandler(KEY, a -> {
            if (step == null || step.getToController() == null || !(a instanceof KeyEvent ke))
                return;

            if (ke.getKey().equals(CDialog.DIALOG_SHOWN) || ke.getKey().equals(UI.WINDOW_OPEN)){
                if (a.getSource().getClass().equals(step.getToController()))
                    confirm(ConfirmState.NEXT);
            }
            else if (ke.getKey().equals(CDialog.DIALOG_HIDE) && ((CTab)Main.getMain().getTab().getSelectionModel().getSelectedItem()).getController().getClass().equals(step.getToController())){
                confirm(ConfirmState.NEXT);
            }
            else if (ke.getKey().equals(UI.WINDOW_CLOSE)){

            }
        }, true);
    }

    public void set(Step step, Consumer<ConfirmState> onConfirm) {
        this.step = step;
        this.onConfirm = onConfirm;

        reload(node1);
        reload(node2);
    }

    public void reload(Node node){
        if (step == null)
            return;

        var txtDescription = (TextArea)node.lookup("#txtDescription");
        var btnNext = (CButton)node.lookup("#btnNext");
        var btnPrevious = (CButton)node.lookup("#btnPrevious");
        var btnCancel = (CButton)node.lookup("#btnCancel");

        //btnNext.setText(step.isEndStep() ? Translator.translate("option.finish") : Translator.translate("option.next"));
        btnNext.setText(step.isEndStep() ? "✓" : ">");
        btnPrevious.setVisible(!step.isFirstStep());

        txtDescription.setText(step.getDescription(Configurator.getConfig().getLanguage()));
        btnNext.setOnMouseClicked(a -> confirm(ConfirmState.NEXT));
        btnPrevious.setOnMouseClicked(a -> confirm(ConfirmState.PREVIOUS));
        btnCancel.setOnMouseClicked(a -> confirm(ConfirmState.CANCEL));

        popup.setWidth(getWidth());
        popup.setHeight(getHeight());

        if (step.getMouseButton() != null)
            Main.getMain().getRootNode().addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEventFilter);
        else if (step.getKeyCode() != null)
            Main.getMain().getRootNode().addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, keyEventFilter);
    }

    private void confirm(ConfirmState val){
        Main.getMain().getRootNode().removeEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, keyEventFilter);
        Main.getMain().getRootNode().removeEventFilter(MouseEvent.MOUSE_CLICKED, mouseEventFilter);

        setVisible(false);
        popup.hide();
        if (onConfirm != null)
            onConfirm.accept(val);

        if (val == ConfirmState.CANCEL){
            step = null;
            onConfirm = null;
        }
    }

    private void keyEventFilter(javafx.scene.input.KeyEvent keyEvent) {
        if (keyEvent.getCode() != step.getKeyCode() || step.getToController() != null)
            return;

        confirm(ConfirmState.NEXT);
    }

    private void mouseEventFilter(MouseEvent event) {
        if (event.getButton() != step.getMouseButton() || step.getToController() != null)
            return;

        confirm(ConfirmState.NEXT);
    }


    public void move(double x, double y){
        var nd = (VBox)node1;

        if (x + nd.getWidth() > getScene().getWidth() || y + nd.getHeight() > getScene().getHeight()){
            setVisible(false);
            popup.hide();
            popup.setX(getScene().getWindow().getX() + x);
            popup.setY(getScene().getWindow().getY() + y);
            popup.show(nd.getScene().getWindow());
            return;
        }

        setVisible(false);
        popup.hide();

        setLayoutX(x);
        setLayoutY(y);

        transition.playFromStart();
        setVisible(true);
    }

    public void dispose(){
        Main.getMain().getHandler().removeHandler(KEY);
        UI.getUI().getHandler().removeHandler(KEY);
    }


}
