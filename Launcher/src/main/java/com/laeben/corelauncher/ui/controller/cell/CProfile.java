package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.core.entity.Path;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.FDObject;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.ui.util.ProfileUtil;
import com.laeben.corelauncher.util.ImageCacheManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class CProfile extends CDockObject{

    public static final String REMOVE = "remove";

    @FXML
    private AnchorPane imgRoot;
    @FXML
    private CButton btnSelect;

    private Path p1, p2;
    private Image img;
    private CDockObject on;

    private final FadeTransition hover;
    private final TranslateTransition trns;

    private Predicate<String> onMenuClick;

    public CProfile(){
        hover = new FadeTransition();
        hover.setFromValue(0);
        hover.setToValue(1);
        hover.setDuration(animationDuration);
        getStyleClass().add("cprofile");

        trns = new TranslateTransition();
        trns.setFromY(90);
        trns.setToY(0);
        trns.setDuration(animationDuration);
    }

    @Override
    protected void onMouseEntered(MouseEvent e) {
        hover.playFromStart();
        trns.playFromStart();

        btnSelect.setStyle("-fx-text-fill: white");
    }

    @Override
    protected void onMouseExited(MouseEvent e) {
        hover.playFromStart();
        trns.playFromStart();
        hover.jumpTo(Duration.ZERO);
        trns.jumpTo(Duration.ZERO);
        hover.stop();
        trns.stop();

        btnSelect.setText("⯈");
    }

    @Override
    protected void onVanished(ActionEvent e){
        if (object == null)
            return;
        if (parent != null && !parent.isSingle())
            FloatDock.getDock().removeFromGroup(parent, getPrimaryProfile());
        else
            FloatDock.getDock().remove(object);

        if (listener != null)
            listener.accept(new KeyEvent(FloatDock.REMOVE));
    }

    @Override
    protected void onMouseReleased(MouseEvent e){
        if (e.getButton() == MouseButton.MIDDLE){
            if (getPrimaryProfile() != null)
                OSUtil.open(getPrimaryProfile().getPath().toFile());
        }
        else{
            btnSelect.setText("♥");
            btnSelect.setStyle("-fx-text-fill: #e30e0e");
            selectPrimary();
        }
    }

    @Override
    protected boolean onSet(FDObject item) {
        var profile = item
                .getProfiles()
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (profile == null)
            return false;

        p1 = profile.getPath().to("profile.json");
        p2 = profile.getPath().to(StrUtil.pure(profile.getName()) + ".json");

        img = ImageCacheManager.getImage(profile, 32);
        var img_128 = ImageCacheManager.getImage(profile, 128);

        menu.clear();

        var ghostButton = new CButton();
        ghostButton.setVisible(false);
        ghostButton.setManaged(false);

        generateProfileMenu(menu, profile, ghostButton, a -> {
            if (a.equals(CDockObject.DELETE))
                vanish();

            return onMenuClick == null || onMenuClick.test(a);
        });
        menu.addItem(ImageCacheManager.getImage("remove.png", 48), REMOVE, Translator.translate("option.remove"), a -> vanish());

        //btnSelect.setOnDragDetected(a -> drag(p1, p2, img_30));
        //btnSelect.setOnMouseReleased(a -> selectPrimary());

        root.setOnDragDone(a -> {
            if (p2.exists())
                p2.delete();
        });

        var bg = new Background(new BackgroundImage(img_128, null, null, null, null));
        imgRoot.setBackground(bg);

        imgRoot.widthProperty().addListener(a -> {
            double pad = (root.getWidth() - 128)/2;
            AnchorPane.setRightAnchor(menu, pad);
            var rect = new Rectangle(128, 128);
            rect.setX(pad);
            rect.setArcHeight(30);
            rect.setArcWidth(30);
            imgRoot.setClip(rect);
        });

        trns.setNode(btnSelect);

        imgRoot.setVisible(true);
        imgRoot.setManaged(true);

        return true;
    }

    @Override
    protected void drag() {
        var p = getPrimaryProfile();
        if (p == null)
            return;

        var board = root.startDragAndDrop(TransferMode.MOVE);
        board.setDragView(img);

        var content = new ClipboardContent();
        content.put(dataFormat, p.getName());
        content.put(DataFormat.PLAIN_TEXT, "inner");

        //p1.copy(p2);
        ProfileUtil.exportJson(p, p2);
        content.putFiles(List.of(p2.toFile()));

        board.setContent(content);
    }

    public CProfile setOnMenuClick(Predicate<String> onMenuClick){
        this.onMenuClick = onMenuClick;
        return this;
    }

    public CDockObject getOnObject(){
        return on;
    }

    public void setOnObject(CDockObject on){
        this.on = on;
    }

    @Override
    public void dispose(){
        onMenuClick = null;
        super.dispose();
    }
}
