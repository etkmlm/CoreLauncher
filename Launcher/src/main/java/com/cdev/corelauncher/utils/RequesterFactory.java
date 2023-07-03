package com.cdev.corelauncher.utils;


public class RequesterFactory{
    private final String baseUrl;
    private boolean http;
    public RequesterFactory(String baseUrl){
        if (baseUrl.startsWith("http://"))
            http = true;

        this.baseUrl = baseUrl.replace("https://", "").replace("http://", "");
    }

    public Requester create(){
        return http ? new Requester(baseUrl).http() : new Requester(baseUrl);
    }
}
