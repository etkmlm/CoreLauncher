package com.laeben.corelauncher.web;

import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.ui.controller.page.WebPage;
import com.laeben.corelauncher.web.cache.JSONCacheStore;
import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.net.CookieStore;
import java.util.function.Consumer;

public class WebComplex {
    public static final int NO_TIMEOUT = -1;

    public enum State{
        OK, MISSING_LIBRARIES, UNKNOWN_ERROR, LOADING
    }

    public static final WebComplex UNKNOWN_ERROR = new WebComplex(State.UNKNOWN_ERROR);
    public static final WebComplex MISSING_LIBRARIES = new WebComplex(State.MISSING_LIBRARIES);

    public record LocationChangedEvent(WebComplex complex, String oldLocation, String location){
        public boolean isLocationEmpty(){
            return location == null;
        }
        public boolean isLocationBlank(){
            return "about:blank".equals(location);
        }
    }
    public record TimeoutReachedEvent(WebComplex complex, String location){}

    private State state = State.LOADING;

    private final WebView view;
    private WebPage attachedPage;

    private Consumer<LocationChangedEvent> onLocationChanged;
    private Consumer<TimeoutReachedEvent> onTimeoutReached;

    public WebComplex(State state){
        this.state = state;
        view = null;
    }

    public WebComplex(CookieStore cookieStore) {
        this(cookieStore, NO_TIMEOUT);
    }

    public WebComplex(CookieStore cookieStore, long timeout){
        view = new WebView();

        view.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            Logger.getLogger().logDebug("Web Complex State: " + (newValue == null ? null : newValue.name()));
        });

        if (timeout > 0){
            getPauseTransition(timeout).play(); // start timeout timer
        }

        view.getEngine().documentProperty().addListener((observable, oldValue, newValue) -> {
            if (onLocationChanged != null)
                onLocationChanged.accept(new LocationChangedEvent(this, oldValue == null ? null : oldValue.getDocumentURI(), newValue == null ? null : newValue.getDocumentURI()));

            if (cookieStore instanceof JSONCacheStore jcs) {
                jcs.save();
            }
        });

        state = State.OK;
    }

    /**
     * Creates a pause transition.
     * @param timeout timeout in millis
     */
    private PauseTransition getPauseTransition(long timeout) {
        var t = new PauseTransition(Duration.millis(timeout));
        if (view != null){
            t.setOnFinished(e -> {
                if (view.getEngine().getLoadWorker().getState() == Worker.State.RUNNING) {
                    Logger.getLogger().logDebug("Timeout reached for a web complex. (URL: '%s')".formatted(view.getEngine().getLocation()));
                    if (onTimeoutReached != null)
                        onTimeoutReached.accept(new TimeoutReachedEvent(this, view.getEngine().getLocation()));
                }
            });
        }
        return t;
    }

    public WebComplex onLocationChanged(Consumer<LocationChangedEvent> onLocationChanged) {
        this.onLocationChanged = onLocationChanged;
        return this;
    }

    public WebComplex onTimeoutReached(Consumer<TimeoutReachedEvent> onTimeoutReached) {
        this.onTimeoutReached = onTimeoutReached;
        return this;
    }

    public WebComplex navigate(String url){
        if (view != null){
            Logger.getLogger().logDebug("Web Complex: Loading URL...");
            view.getEngine().load(url);
            Logger.getLogger().logDebug("Web Complex: Loaded URL.");
        }
        else{
            Logger.getLogger().logDebug("Web Complex: Cannot load URL (not loaded).");
        }
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

    public State getState(){
        return state;
    }
}
