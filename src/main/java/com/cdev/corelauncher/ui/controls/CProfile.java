package com.cdev.corelauncher.ui.controls;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.CoreLauncherFX;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.ui.controller.Main;
import com.cdev.corelauncher.ui.utils.FXManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import org.controlsfx.control.action.Action;

import java.io.IOException;

public class CProfile extends ListCell<Profile> {

    private final Node gr;

    public CProfile(){
        var n = CoreLauncherFX.class.getResource("/com/cdev/corelauncher/entities/cprofile.fxml");
        FXMLLoader loader = new FXMLLoader(n);
        loader.setController(this);
        try{
            setGraphic(gr = loader.load());
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @FXML
    private ImageView profileImage;
    @FXML
    private Label lblProfileName;
    @FXML
    private Label lblProfileVersion;
    @FXML
    private Label lblProfileDescription;
    @FXML
    private ContextMenu contextMenu;
    @FXML
    private Button btnContextMenu;
    @FXML
    private Button btnActive;



    @Override
    protected void updateItem(Profile profile, boolean empty) {
        super.updateItem(profile, empty);

        if (profile == null || empty){
            setGraphic(null);
            return;
        }

        lblProfileName.setText(profile.getName());
        lblProfileVersion.setText(profile.getVersionId());


        setGraphic(gr);
    }

    //------------------------

    public void sayHi(MouseEvent event)
    {
        System.out.printf("selam");
    }

    public void onBtnCMClicked(ActionEvent event)
    {

        double screenX = btnContextMenu.localToScreen(btnContextMenu.getBoundsInLocal()).getMinX();
        double screenY = btnContextMenu.localToScreen(btnContextMenu.getBoundsInLocal()).getMinY();
        contextMenu.show((Button)event.getSource(), screenX + 10, screenY + 10);

    }

    public void onBtnActiveClicked(ActionEvent event) {

        if (btnActive.getTextFill() == Paint.valueOf("red")) {
            btnActive.setTextFill(Paint.valueOf("White"));

        } else {
            btnActive.setTextFill(Paint.valueOf("red"));
        }

        System.out.println("btnActive clicked");

    }

    public void MenuItemCompleteB(ActionEvent event) {

        System.out.println("Complete Backup");

    }

    public void MenuItemSend(ActionEvent event) {

        System.out.println("send");

    }

    public void MenuItemOpenFolder(ActionEvent event) {

        System.out.println("Open Folder");
    }

    public void MenuItemEdit(ActionEvent event) {

        System.out.println("edit");
    }

    public void MenuItemDelete(ActionEvent event) {
        System.out.printf("delete profile '" + lblProfileName.getText() + "'");
    }


}
