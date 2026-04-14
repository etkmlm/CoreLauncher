package com.laeben.corelauncher.web;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.util.NativeManager;
import com.laeben.corelauncher.util.entity.LogType;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;

public record EmbeddedBrowser(CookieStore cookieStore) {
    private static EmbeddedBrowser instance;

    public static EmbeddedBrowser getInstance() {
        return instance;
    }

    public EmbeddedBrowser(CookieStore cookieStore) {
        CookieManager manager = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
        this.cookieStore = cookieStore;

        instance = this;
    }

    public static void downloadNativeLibraries() throws NoConnectionException, HttpException, StopException {
        NativeManager.downloadModule("org/openjfx", "javafx-web", "21.0.7");
        NativeManager.downloadModule("org/openjfx", "javafx-media", "21.0.7");

        Logger.getLogger().log(LogType.INFO, "Restarting the launcher...");
        CoreLauncher.restart();
    }

    public WebComplex getWebComplex() {
        return getWebComplex(WebComplex.NO_TIMEOUT);
    }

    public WebComplex getWebComplex(long timeout) {
        try{
            return new WebComplex(cookieStore, timeout);
        }
        catch (UnsatisfiedLinkError | NoClassDefFoundError error){
            final String msg = error.getMessage();
            if (msg.contains("jfxwebkit") || msg.contains("jfxmedia") || msg.contains("WebPage")){ // no web library
                return WebComplex.MISSING_LIBRARIES;
            }

            Logger.getLogger().log(LogType.ERROR, "Unknown missing libraries for web complex: " + msg);

            return WebComplex.UNKNOWN_ERROR;
        }
        catch (Exception t){
            Logger.getLogger().log(t);

            return WebComplex.UNKNOWN_ERROR;
        }
    }
}
