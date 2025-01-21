package com.laeben.corelauncher.util;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.entity.ImageEntity;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.util.entity.ImageTask;
import javafx.scene.image.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ImageUtil {
    public static final Image DEFAULT_IMAGE_128 = getLocalImage("creeper.png", 128, 128);
    public static final Image DEFAULT_IMAGE_48 = getLocalImage("creeper.png", 48, 48);
    public static final Image DEFAULT_IMAGE_32 = getLocalImage("creeper.png", 32, 32);

    private static final Executor asyncExecutor = Executors.newSingleThreadExecutor();

    public static Image getImageSync(ImageTask task, boolean useDefault){
        asyncExecutor.execute(task);
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            Logger.getLogger().log(e);
            return getDefaultImage(task.getRequestedWidth());
        }
    }
    public static void getImageAsync(ImageTask task, Consumer<Image> onDone, boolean useDefault){
        task.setOnSucceeded(a -> onDone.accept(task.getValue()));
        task.setOnFailed(e -> {
            Logger.getLogger().log(e.getSource().getException());
            onDone.accept(useDefault ? getDefaultImage(task.getRequestedWidth() < 0 ? 128 : task.getRequestedWidth()) : null);
        });
        asyncExecutor.execute(task);
    }

    public static Image getLocalImage(String name){
        return getLocalImage(name, -1, -1);
    }
    public static Image getLocalImage(String name, double width, double height){
        var x = CoreLauncherFX.class.getResource("images/" + name);
        if (x == null)
            return null;

        try {
            return width < 0 ? new Image(x.openStream()) : new Image(x.openStream(), width, height, false, false);
        } catch (IOException e) {
            return null;
        }
    }
    public static boolean extractLocalImage(String name, Path to){
        var x = CoreLauncherFX.class.getResource("images/" + name);
        if (x == null)
            return false;

        try (var str = x.openStream();
             var file = new FileOutputStream(to.toFile())) {
            file.write(str.readAllBytes());
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static ImageTask getNetworkImage(String url, double w, double h){
        return new ImageTask(w, h) {
            @Override
            protected Image evaluate() throws Exception {
                if (url.endsWith(".webp"))
                    return awtToFXImage(ImageIO.read(new URL(url)), w, h);
                else
                    return w < 0 ? new Image(url, true) : new Image(url, w, h, false, false, true);
            }
        };
    }

    public static ImageTask getImage(ImageEntity entity){
        return getImage(entity, -1, -1);
    }
    public static ImageTask getImage(ImageEntity entity, double w, double h){
        var defaultTask = ImageTask.fromImage(getDefaultImage(w < 0 ? 128 : w), w, w);

        if (entity == null)
            return defaultTask;

        if (entity.isNetwork()){
            return getNetworkImage(entity.getUrl(), w, h);
        }
        else if (entity.isEmbedded()){
            return ImageTask.fromImage(getLocalImage(entity.getIdentifier(), w, h));
        }
        else{
            if (entity.isBase64()){
                return new ImageTask() {
                    @Override
                    protected Image evaluate() throws Exception {
                        try(var str = new ByteArrayInputStream(ImageCacheManager.decodeImage(entity.getIdentifier()))){
                            return w < 0 ? new Image(str) : new Image(str, w, h, false, false);
                        }
                    }
                };
            }
            var path = entity.getPath(Configurator.getConfig().getImagePath());
            if (!path.exists() && entity.getUrl() != null)
                NetUtil.downloadImage(path, entity.getUrl(), false);

            if (entity.getIdentifier().endsWith(".webp")){
                return new ImageTask() {
                    @Override
                    protected Image evaluate() throws Exception {
                        return awtToFXImage(ImageIO.read(path.toFile()), w, h);
                    }
                };
            }

            return new ImageTask() {
                @Override
                protected Image evaluate() throws Exception {
                    try (var r = new FileInputStream(path.toFile())) {
                        return w < 0 ? new Image(r) : new Image(r, w, h, false, false);
                    }
                }
            };
        }
    }

    private static Image awtToFXImage(java.awt.Image img, double w, double h) {
        if (w >= 0)
            img = img.getScaledInstance((int)w, (int)h, java.awt.Image.SCALE_FAST);

        var buffImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        buffImg.createGraphics().drawImage(img, 0, 0, null);

        var buffer = IntBuffer.wrap(((DataBufferInt)buffImg.getRaster().getDataBuffer()).getData());

        final var pixelFormat = PixelFormat.getIntArgbPreInstance();

        return new WritableImage(new PixelBuffer<>(buffImg.getWidth(), buffImg.getHeight(), buffer, pixelFormat));
    }

    public static Image getDefaultImage(double size){
        return switch ((int)size){
            case 32 -> DEFAULT_IMAGE_32;
            case 48 -> DEFAULT_IMAGE_48;
            default -> DEFAULT_IMAGE_128;
        };
    }

    public static ImageTask getImageFromProfile(Profile p, double w, double h){
        if (Profiler.verifyProfileIcon(p)){
            ImageCacheManager.remove(p);
            p.save();
        }

        return getImage(p.getIcon(), w, h).then(a -> {
            if (p.getIcon() != null && !p.getIcon().isNetwork() && p.getIcon().getUrl() != null)
                ImageCacheManager.remove(p);
            return a;
        });
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
}
