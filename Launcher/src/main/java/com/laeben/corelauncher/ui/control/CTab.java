package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.api.entity.ImageEntity;
import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.util.entity.ImageKeyProvider;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class CTab extends Tab {
    private static class CTabImageKeyProvider implements ImageKeyProvider {
        private String iconName;

        public void setIconName(String iconName) {
            this.iconName = iconName;
        }

        public void removeCache(){
            if (iconName != null)
                ImageCacheManager.remove(this);
        }

        @Override
        public String getKey() {
            return iconName;
        }
    }
    private Controller controller;

    public void setController(Controller c){
        this.controller = c;
    }

    public Controller getController(){
        return controller;
    }

    private CTabImageKeyProvider imageKeyProvider;
    private Pane headerRegion;
    public Pane getHeaderRegion(){
        return headerRegion;
    }
    public void setHeaderRegion(Pane headerRegion){
        this.headerRegion = headerRegion;
    }

    private Color headerColor = null;
    private ImageEntity imageIcon = null;
    private String oldText = null;

    public boolean hasIcon(){
        return getGraphic() != null;
    }

    public boolean hasHeaderColor(){
        return headerColor != null;
    }

    public Color getHeaderColor(){
        return headerColor;
    }

    public void setHeaderColor(Color color){
        headerColor = color;
        if (headerRegion != null)
            headerRegion.setStyle(color == null ? null : "-fx-background-color: #" + color.toString().substring(2));
    }

    public void setIcon(Node graphic){
        setGraphic(graphic);

        if (imageKeyProvider != null)
            imageKeyProvider.setIconName(null);

        if (graphic == null){
            if (oldText != null){
                setText(oldText);
                oldText = null;
            }
        }
        else if (!getText().isEmpty() && getText().isBlank() && oldText == null){ // preserve tab width
            oldText = getText();
            setText(" ");
        }
    }

    public ImageEntity getImageIcon(){
        return imageIcon;
    }

    public void setIconAsync(ImageEntity image){
        imageIcon = image;

        if (image == null) {
            setIcon(null);
            return;
        }

        var view = new CView();
        setIcon(view);

        view.setImageAsync(ImageUtil.getImage(image, 24, 24));
    }

    public void setIconWithName(String name){
        if (name == null){
            setIcon(null);
            return;
        }

        if (imageKeyProvider != null) imageKeyProvider.removeCache();
        else imageKeyProvider = new CTabImageKeyProvider();

        imageKeyProvider.setIconName(name);
        final var view = new CView();
        view.setImage(ImageCacheManager.getImage(this, 16));

        setIcon(view);
    }

    public String getKey() {
        return getController() instanceof HandlerController hc ? hc.getKeyFamily() : (getText() != null ? getText() : "tab" + hashCode());
    }

    public void dispose(){
        if (controller != null)
            controller.dispose();
    }
}
