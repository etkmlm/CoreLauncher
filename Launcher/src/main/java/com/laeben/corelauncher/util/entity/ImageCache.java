package com.laeben.corelauncher.util.entity;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.entity.Profile;
import javafx.scene.image.Image;

public interface ImageCache<T> {
    int getSize();
    Image getCachedImage();
    Image getImage();
    T getKey();

    static <T> ImageCache<T> get(T key, int size){
        if (key instanceof Profile p)
            return (ImageCache<T>) new ProfileImageCache(p, size);
        else
            return (ImageCache<T>) new StringImageCache(key.toString(), size);
    }

    class ProfileImageCache implements ImageCache<Profile>{

        private final Profile profile;
        private final int size;
        private final Image image;

        public ProfileImageCache(Profile profile, int size){
            this.profile = profile;
            this.size = size;
            this.image = getImage();
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public Image getCachedImage() {
            return image;
        }

        @Override
        public Image getImage() {
            return CoreLauncherFX.getImageFromProfile(profile, size, size);
        }

        @Override
        public Profile getKey() {
            return profile;
        }
    }

    class StringImageCache implements ImageCache<String>{
        private final String key;
        private final int size;
        private final Image image;
        public StringImageCache(String key, int size){
            this.key = key;
            this.size = size;
            this.image = getImage();
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public Image getCachedImage() {
            return image;
        }

        @Override
        public Image getImage() {
            return CoreLauncherFX.getLocalImage(key);
        }

        @Override
        public String getKey() {
            return key;
        }
    }
}
