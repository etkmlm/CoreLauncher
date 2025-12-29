package com.laeben.corelauncher.web;

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

    public WebComplex getWebComplex() {
        return new WebComplex(cookieStore);
    }
}
