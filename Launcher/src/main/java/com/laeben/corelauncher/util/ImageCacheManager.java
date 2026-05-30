package com.laeben.corelauncher.util;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.util.entity.ImageCache;
import javafx.scene.image.Image;

import java.util.*;
import java.util.function.Supplier;

public class ImageCacheManager {
    private static final List<ImageCache> cache = new ArrayList<>();

    public static <T> void remove(T k){
        cache.removeIf(a -> a.getKey().equals(k));
    }

    public static void clear(){
        cache.clear();
    }

    public static <T> Image getImage(T k, int size, Supplier<ImageCache> createCache){
        var x = cache.stream().filter(a -> a.getKey().equals(k) && a.getSize() == size).findFirst();
        if(x.isPresent())
            return x.get().getCachedImage();

        var c = createCache.get();
        cache.add(c);
        return c.getCachedImage();
    }

    public static <T> Image getImage(T k, int size){
        return getImage(k, size, () -> ImageCache.get(k, size));
    }

    public static String encodeImage(Path img){
        var bytes = img.readBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] decodeImage(String encoded){
        return Base64.getDecoder().decode(encoded);
    }
}
