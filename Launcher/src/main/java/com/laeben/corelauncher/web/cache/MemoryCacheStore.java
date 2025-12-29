package com.laeben.corelauncher.web.cache;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class MemoryCacheStore implements CookieStore {
    private final Map<String, List<HttpCookie>> cookies;
    private static MemoryCacheStore instance;

    public MemoryCacheStore() {
        this.cookies = new HashMap<>();

        instance = this;
    }

    public static MemoryCacheStore getInstance() {
        return instance;
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        var uriString = uri.getHost();
        if (cookies.containsKey(uriString))
            cookies.get(uriString).add(cookie);
        else
            cookies.put(uriString, new ArrayList<>(){{add(cookie);}});
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        return cookies.getOrDefault(uri.getHost(), new ArrayList<>());
    }

    @Override
    public List<HttpCookie> getCookies() {
        return cookies.values().stream().flatMap(Collection::stream).toList();
    }

    @Override
    public List<URI> getURIs() {
        var list = new ArrayList<URI>();
        for (var entry : cookies.keySet()){
            try{
                list.add(new URI(entry));
            } catch (URISyntaxException ignored) {}
        }

        return list;
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        var uriString = uri.getHost();

        if (cookies.containsKey(uriString))
            return cookies.get(uriString).remove(cookie);

        return false;
    }

    @Override
    public boolean removeAll() {
        cookies.clear();
        return true;
    }
}