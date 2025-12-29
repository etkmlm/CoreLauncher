package com.laeben.corelauncher.web;

import com.laeben.corelauncher.ui.controller.page.WebPage;
import com.laeben.corelauncher.web.cache.JSONCacheStore;
import javafx.scene.web.WebView;

import java.net.CookieStore;
import java.util.function.Consumer;

public class WebComplex {
    public record LocationChangedEvent(WebComplex complex, String oldLocation, String location){}

    private final WebView view;
    private WebPage attachedPage;

    private Consumer<LocationChangedEvent> onLocationChanged;

    public WebComplex(CookieStore cookieStore){
        view = new WebView();

        view.getEngine().documentProperty().addListener((observable, oldValue, newValue) -> {
            if (onLocationChanged != null)
                onLocationChanged.accept(new LocationChangedEvent(this, oldValue == null ? null : oldValue.getDocumentURI(), newValue == null ? null : newValue.getDocumentURI()));

            if (cookieStore instanceof JSONCacheStore jcs) {
                jcs.save();
            }
        });
    }

    public WebComplex onLocationChanged(Consumer<LocationChangedEvent> onLocationChanged) {
        this.onLocationChanged = onLocationChanged;
        return this;
    }

    public WebComplex navigate(String url){
        view.getEngine().load(url);
        return this;
    }

    public void attachPage(WebPage page){
        attachedPage = page;
    }

    public WebPage getAttachedPage(){
        return attachedPage;
    }

    public WebView getView(){
        return view;
    }
}
