package com.laeben.corelauncher.api.entity;

import com.laeben.core.entity.Path;

import java.net.MalformedURLException;
import java.util.UUID;

public class ImageEntity {
    private boolean network;
    private boolean embedded;
    private boolean base64;
    private String url;
    private String identifier;

    private ImageEntity(String url, String identifier) {
        network = true;
        this.url = url;
        this.identifier = identifier;
    }

    public static ImageEntity empty(){
        return new ImageEntity(null);
    }

    public boolean isEmpty(){
        return identifier == null;
    }

    private ImageEntity(String identifier){
        network = false;
        this.identifier = identifier;
    }

    public static ImageEntity fromBase64(String text){
        return new ImageEntity(text).asBase64();
    }

    public static ImageEntity fromNetwork(String url) throws MalformedURLException {
        return new ImageEntity(url, UUID.randomUUID() + ".png");
    }

    public static ImageEntity fromLocal(String identifier){
        return new ImageEntity(identifier);
    }

    public static ImageEntity fromEmbedded(String identifier){
        return new ImageEntity(identifier).setEmbedded(true);
    }

    public String getUrl(){
        return url;
    }

    public String getIdentifier(){
        return identifier;
    }

    public ImageEntity asBase64(){
        base64 = true;
        return this;
    }

    public boolean isBase64(){
        return base64;
    }

    public boolean isNetwork(){
        return network;
    }

    public boolean isEmbedded(){
        return embedded;
    }

    public ImageEntity setIdentifier(String identifier){
        this.identifier = identifier;

        return this;
    }

    public ImageEntity setEmbedded(boolean embedded){
        this.embedded = embedded;
        return this;
    }

    public ImageEntity setUrl(String url){
        this.url = url;

        return this;
    }

    public Path getPath(Path parent){
        return parent.to(identifier);
    }
}
