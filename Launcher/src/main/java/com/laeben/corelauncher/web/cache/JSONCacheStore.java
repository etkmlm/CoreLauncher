package com.laeben.corelauncher.web.cache;

import com.google.gson.*;
import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.Configurator;

import java.lang.reflect.Type;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class JSONCacheStore implements CookieStore {
    public static final class JSONCacheFactory implements JsonSerializer<Map<String, List<HttpCookie>>>, JsonDeserializer<Map<String, List<HttpCookie>>>{
        @Override
        public Map<String, List<HttpCookie>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var map = new HashMap<String, List<HttpCookie>>();
            if (!json.isJsonObject())
                return map;

            for(var entry : json.getAsJsonObject().entrySet()){
                var cookies = new ArrayList<HttpCookie>();
                if (entry.getValue().isJsonObject()){
                    for (var cookieEntry : entry.getValue().getAsJsonObject().entrySet()){
                        if (!cookieEntry.getValue().isJsonObject())
                            continue;
                        var cookie = cookieEntry.getValue().getAsJsonObject();
                        var value = cookie.get("value");
                        var age = cookie.get("age");
                        var path = cookie.get("path");
                        var domain = cookie.get("domain");
                        var httpOnly = cookie.get("httpOnly");
                        var version = cookie.get("version");
                        if (value != null){
                            var c = new HttpCookie(cookieEntry.getKey(), value.getAsString().replace("\"", ""));
                            if (age != null && !age.isJsonNull()){
                                if (age.getAsLong() <= 0)
                                    continue; // temporary cookies
                                c.setMaxAge(age.getAsLong());
                            }
                            if (path != null)
                                c.setPath(path.getAsString());
                            if (domain != null)
                                c.setDomain(domain.getAsString());
                            if (httpOnly != null)
                                c.setHttpOnly(!httpOnly.isJsonNull() && httpOnly.getAsBoolean());
                            if (version != null)
                                c.setVersion(version.isJsonNull() ? 0 : version.getAsInt());
                            cookies.add(c);
                        }
                    }
                }
                map.put(entry.getKey(), cookies);
            }

            return map;
        }

        @Override
        public JsonElement serialize(Map<String, List<HttpCookie>> src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null)
                return null;
            var obj = new JsonObject();

            for (var entries : src.entrySet()){
                var key = entries.getKey();
                var cookies = new JsonObject();
                for(var cookie : entries.getValue()){
                    var ck = new JsonObject();
                    ck.add("value", new JsonPrimitive(cookie.getValue().replace("\"", "")));
                    ck.add("age", new JsonPrimitive(cookie.getMaxAge()));
                    ck.add("path", new JsonPrimitive(cookie.getPath()));
                    ck.add("domain", new JsonPrimitive(cookie.getDomain()));
                    ck.add("httpOnly", new JsonPrimitive(cookie.isHttpOnly()));
                    ck.add("version", new JsonPrimitive(cookie.getVersion()));
                    cookies.add(cookie.getName(), ck);
                }
                obj.add(key, cookies);
            }

            return obj;
        }
    }

    public static final String KEY = "jsoncachestr";

    private final Gson gson;

    private Map<String, List<HttpCookie>> cookies;
    private Path filePath;

    private JSONCacheStore(Map<String, List<HttpCookie>> cookies) {
        gson = null;
        this.cookies = cookies;
    }

    public JSONCacheStore() {
        gson = new GsonBuilder().registerTypeAdapter(Map.class, new JSONCacheFactory()).create();

        Configurator.getConfigurator().getHandler().addHandler(KEY, (e) -> {
            if (!e.getKey().equals(Configurator.GAME_PATH_CHANGE))
                return;

            filePath = filePath();
            reload();
        }, true);

        reload();
    }

    private Path filePath(){
        return Configurator.getConfig().getLauncherPath().to("browser", "cookies.json");
    }

    public void reload(){
        if (filePath == null)
            filePath = filePath();

        cookies = gson.fromJson(filePath.read(), Map.class);
        if (cookies == null)
            cookies = new HashMap<>();
    }

    public void save(){
        if (filePath == null || cookies == null)
            return;

        filePath.write(gson.toJson(cookies, Map.class));
    }

    private static String getURIDomain(URI uri){
        return uri.getHost();
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        if (cookies == null)
            return;

        var uriString = getURIDomain(uri);

        if (cookies.containsKey(uriString)){
            var g = cookies.get(uriString);
            boolean found = false;

            for (int i = 0; i < g.size(); i++){
                var c = g.get(i);
                if (c.getName().equals(cookie.getName())){
                    found = true;
                    g.set(i, cookie);
                    break;
                }
            }

            if (!found)
                g.add(cookie);
        }
        else
            cookies.put(uriString, new ArrayList<>(List.of(cookie)));
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        if (cookies == null)
            return List.of();
        return cookies.getOrDefault(getURIDomain(uri), new ArrayList<>());
    }

    @Override
    public List<HttpCookie> getCookies() {
        if (cookies == null)
            return List.of();
        return cookies.values().stream().flatMap(Collection::stream).toList();
    }

    @Override
    public List<URI> getURIs() {
        if (cookies == null)
            return List.of();

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
        if (cookies == null)
            return false;

        var uriString = getURIDomain(uri);

        if (cookies.containsKey(uriString))
            return cookies.get(uriString).removeIf(c -> c.getName().equals(cookie.getName()));

        return false;
    }

    @Override
    public boolean removeAll() {
        if (cookies == null)
            return false;

        cookies.clear();
        return true;
    }
}
