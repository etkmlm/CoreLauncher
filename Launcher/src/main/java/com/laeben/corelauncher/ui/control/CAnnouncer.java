package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.util.Duration;


public class CAnnouncer extends Parent {
    @FXML
    public Label lblTitle;
    @FXML
    public Text txtContent;
    @FXML
    public CView icon;

    private Node node;

    private final Timeline line;

    private final int duration = 500;

    public CAnnouncer(){
        node = UI.getUI().load(CoreLauncherFX.class.getResource("layout/controls/cannouncer.fxml"), this);
        getChildren().add(node);
        line = new Timeline(
                new KeyFrame(Duration.millis(duration), new KeyValue(translateXProperty(), 0)),
                new KeyFrame(Duration.millis(0)),
                new KeyFrame(Duration.millis(0))
        );
        /*trns = new TranslateTransition();
        trns.setFromX(-600);
        trns.setToX(0);
        trns.setDuration(Duration.millis(200));
        trns.setNode(node);*/
    }


    public void announce(Announcement a, Duration d){
        lblTitle.setText(a.title());
        txtContent.setText(a.content());
        icon.setImage(switch (a.type()){
            case GAME -> ImageUtil.getLocalImage("vanilla.png");
            case INFO -> ImageUtil.getLocalImage("announcer/info.png");
            case ERROR -> ImageUtil.getLocalImage("announcer/error.png");
            default -> ImageUtil.getLocalImage("announcer/broadcast.png");
        });
        var d1 = duration + d.toMillis();
        line.getKeyFrames().set(1, new KeyFrame(Duration.millis(d1), new KeyValue(translateXProperty(), 0)));
        line.getKeyFrames().set(2, new KeyFrame(Duration.millis(d1 + duration), new KeyValue(translateXProperty(), 620)));
        line.playFromStart();
    }
}
