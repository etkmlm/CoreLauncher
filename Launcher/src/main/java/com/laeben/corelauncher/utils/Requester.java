package com.laeben.corelauncher.utils;

import com.laeben.corelauncher.utils.entities.NoConnectionException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Requester {
    public record Parameter(String key, Object value) {

        public static Parameter bearer(String token) {
            return new Parameter("Authorization", "Bearer " + token);

        }

        public static Parameter contentType(String type){
            return new Parameter("Content-Type", type);
        }

        public static Parameter fromString(String str) {
            var spl = str.split(":");
            return new Parameter(spl[0], spl[1].trim());
        }

        public static <T> List<Parameter> classToParams(T c, Class<T> t){
            var params = new ArrayList<Parameter>();

            for (var f : t.getDeclaredFields()) {
                try {
                    Object a = f.get(c);
                    if (a == null || (a instanceof Integer i && i == -1))
                        continue;
                    if (a instanceof Enum<?> e)
                        a = e.ordinal();
                    params.add(new Parameter(f.getName(), a));
                } catch (IllegalAccessException ignored) {

                }
            }

            return params;
        }

        @Override
        public String toString() {
            return key + ": " + value;
        }
    }
    private final List<Parameter> headers;
    private final List<Parameter> parameters;
    private String start = "https://";
    private String url;

    public Requester(String baseUrl){
        url = baseUrl
                .replace("https://", "")
                .replace("http://", "");

        if (!url.endsWith("/"))
            url += "/";

        headers = new ArrayList<>();
        parameters = new ArrayList<>();
    }

    public Requester http(){
        start = "http://";
        return this;
    }

    public Requester to(String url){
        this.url += url;
        this.url = this.url.replace("//", "/");
        return this;
    }

    private String getParams(){
        return "?" + String.join("&", parameters.stream().map(x -> x.key + "=" + x.value).toArray(String[]::new));
    }

    public Requester withHeader(Parameter h){
        headers.add(h);
        return this;
    }

    public Requester withParam(Parameter p){
        parameters.add(p);
        return this;
    }

    public Requester withParams(List<Parameter> ps){
        parameters.addAll(ps);

        return this;
    }

    public String getString(){
        try{
            return NetUtils.urlToString(getUrl(), headers.toArray(Parameter[]::new));
        }
        catch (NoConnectionException e){
            return null;
        }

    }

    public String post(String content){
        return NetUtils.post(getUrl(), content, headers.toArray(Parameter[]::new));
    }

    public String getUrl(){
        return start + url + getParams();
    }

    public URL toURL(){
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}