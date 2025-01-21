package com.laeben.corelauncher.util.entity;

import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.scene.image.Image;

public interface ImageCache<T> {
    int getSize();
    Image getCachedImage();
    ImageTask getImage();
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
            this.image = ImageUtil.getImageSync(getImage(), true);
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
        public ImageTask getImage() {
            return ImageUtil.getImageFromProfile(profile, size, size);
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
            this.image = ImageUtil.getImageSync(getImage(), true);
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
        public ImageTask getImage() {
            return ImageTask.fromImage(ImageUtil.getLocalImage(key));
        }

        @Override
        public String getKey() {
            return key;
        }
    }
}
