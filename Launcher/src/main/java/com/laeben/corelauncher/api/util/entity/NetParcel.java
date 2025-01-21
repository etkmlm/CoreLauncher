package com.laeben.corelauncher.api.util.entity;

import com.laeben.core.entity.Path;

public class NetParcel {
    private final String url;
    private final Path path;
    private final boolean uon;

    private Object state;

    private Runnable onFinish;
    private boolean isDone;
    private Exception e;

    private NetParcel(String url, Path path, boolean uon) {
        this.url = url;
        this.path = path;
        this.uon = uon;
    }

    public static NetParcel create(String url, Path path, boolean uon) {
        return new NetParcel(url, path, uon);
    }

    public NetParcel setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    public NetParcel setState(Object st){
        this.state = st;
        return this;
    }

    public <T> T getState(){
        return (T)state;
    }

    public String getUrl(){
        return url;
    }

    public Path getPath(){
        return path;
    }

    public boolean useOriginalName(){
        return uon;
    }

    public void markAsDone(){
        isDone = true;
        e = null;
    }

    public Runnable getOnFinish(){
        return onFinish;
    }

    public Exception getException(){
        return e;
    }

    public void markAsException(Exception e){
        this.e = e;
        isDone = true;
    }
    public boolean isSuccessful(){
        return isDone && e == null;
    }
    public boolean isDone(){
        return isDone;
    }

}
