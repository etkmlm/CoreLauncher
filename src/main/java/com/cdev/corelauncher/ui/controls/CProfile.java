package com.cdev.corelauncher.ui.controls;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.CoreLauncherFX;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.ui.utils.FXManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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

    public void sayHi(ActionEvent event)
    {
        System.out.printf("selam");
    }

    public void onBtnCMClicked(MouseEvent event)
    {
        if (event.isPrimaryButtonDown())
        {

        }


    }


}
