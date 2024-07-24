package com.laeben.corelauncher;

import com.laeben.corelauncher.api.entity.ImageEntity;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.api.util.NetUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Objects;

public class CoreLauncherFX extends Application {

    public static final String CLUI_CSS = CoreLauncherFX.class.getResource("/com/laeben/corelauncher/style/controls/clui.css").toExternalForm();
    public static final Image DEFAULT_IMAGE_128 = CoreLauncherFX.getLocalImage("creeper.png", 128, 128);
    public static final Image DEFAULT_IMAGE_48 = CoreLauncherFX.getLocalImage("creeper.png", 48, 48);
    public static final Image DEFAULT_IMAGE_32 = CoreLauncherFX.getLocalImage("creeper.png", 32, 32);


    @Override
    public void start(Stage stage) throws IOException {
        stage.close();
        new UI();
        new FloatDock().reload();
        Platform.setImplicitExit(false);

        Font.loadFont(Objects.requireNonNull(CoreLauncherFX.class.getResource("/com/laeben/corelauncher/font/Minecraft.ttf")).toExternalForm(), 16);

        CoreLauncher.GUI_INIT = true;

        if (Debug.DEBUG_UI){
            Debug.runUI();
            Platform.exit();
        }
        else
            UI.getUI().create("main").show();

        // Version check
        CoreLauncher.updateCheck();

        new Thread(CoreLauncher::announcementCheck).start();
    }

    public static void launchFX(){
        launch();
    }

    public static Image getImage(ImageEntity entity, double w, double h){
        var dI = getDefaultImage(w < 0 ? 128 : w);

        if (entity == null)
            return dI;

        if (entity.isNetwork()){
            try {
                return w < 0 ? new Image(entity.getUrl(), true) : new Image(entity.getUrl(), w, h, false, false, true);
            } catch (IllegalArgumentException e) {
                return dI;
            }
        }
        else if (entity.isEmbedded()){
            var img = w < 0 ? getLocalImage(entity.getIdentifier()) : getLocalImage(entity.getIdentifier(), w, h);
            return img == null ? dI : img;
        }
        else{
            if (entity.isBase64()){
                try(var str = new ByteArrayInputStream(ImageCacheManager.decodeImage(entity.getIdentifier()))){
                    return w < 0 ? new Image(str) : new Image(str, w, h, false, false);
                } catch (IOException e) {
                    return dI;
                }
            }
            var path = entity.getPath(Configurator.getConfig().getImagePath());
            if (!path.exists() && entity.getUrl() != null)
                NetUtil.downloadImage(path, entity.getUrl(), false);

            try(var r = new FileInputStream(path.toFile())){
                return w < 0 ? new Image(r) : new Image(r, w, h, false, false);
            } catch (IOException e) {
                return dI;
            }
        }
    }

    public static Image getImage(ImageEntity entity){
        return getImage(entity, -1, -1);
    }

    public static Image getDefaultImage(double size){
        return switch ((int)size){
            case 32 -> DEFAULT_IMAGE_32;
            case 48 -> DEFAULT_IMAGE_48;
            default -> DEFAULT_IMAGE_128;
        };
    }

    public static Image getImageFromProfile(Profile p, double w, double h){

        if (Profiler.verifyProfileIcon(p)){
            ImageCacheManager.remove(p);
            p.save();
        }

        var img = getImage(p.getIcon(), w, h);
        if (p.getIcon() != null && !p.getIcon().isNetwork() && p.getIcon().getUrl() != null)
            ImageCacheManager.remove(p);
        return img;
    }

    public static Image resizeImage(Image original, int x, int y, int w, int h, int scaleFactor){
        int newWidth = w * scaleFactor;
        int newHeight = h * scaleFactor;

        var reader = original.getPixelReader();

        int[] fx = new int[w * h];
        reader.getPixels(x, y, w, h, WritablePixelFormat.getIntArgbInstance(), fx, 0, w);

        int[][] nMatrix = new int[newWidth][newHeight];

        for (int i = 0; i < w; i++)
            for (int j = 0; j < h; j++) {
                var color = fx[j * w + i];
                for (int f = 0; f < scaleFactor; f++)
                    for (int s = 0; s < scaleFactor; s++)
                        nMatrix[i * scaleFactor + f][j * scaleFactor + s] = color;
            }

        IntBuffer buffer = IntBuffer.allocate(newWidth * newHeight * 4);

        for (int i = 0; i < newWidth; i++)
            for (int j = 0; j < newHeight; j++){
                buffer.put(j * newWidth + i, nMatrix[i][j]);
            }

        PixelBuffer<IntBuffer> pixBuffer = new PixelBuffer<>(newWidth, newHeight, buffer, WritablePixelFormat.getIntArgbPreInstance());

        return new WritableImage(pixBuffer);
    }

    public static Image getLocalImage(String name){
        var x = CoreLauncherFX.class.getResource("images/" + name);
        if (x == null)
            return null;

        try {
            return new Image(x.openStream());
        } catch (IOException e) {
            return null;
        }
    }
    public static Image getLocalImage(String name, double width, double height){
        var x = CoreLauncherFX.class.getResource("images/" + name);
        if (x == null)
            return null;

        try {
            return new Image(x.openStream(), width, height, false, false);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isAnyPopupOpen(){
        return Window.getWindows().stream().anyMatch(a -> a instanceof Popup);
    }
    public static List<Popup> getAllPopupWindows(){
        return Window.getWindows().stream().filter(a -> a instanceof Popup).map(a -> (Popup)a).toList();
    }
}