package com.laeben.corelauncher.minecraft.entity;

import java.util.regex.Pattern;

public class Asset {
    public String id;
    public String SHA1;
    public int size;
    public int totalSize;
    public String url;
    public String path;

    public transient String base;
    public transient int version;


    public Asset(String id){

    }

    public Asset(String path, String hash, int size){
        this.path = path;
        SHA1 = hash;
        this.size = size;
    }

    public void calculateComparator(){
        final Pattern mtc = Pattern.compile("([0-9.]+).*");

        String[] spl = base.replace("[", "").replace("]", "").split(":");
        String lastPart = spl[spl.length - 1];

        var matcher = mtc.matcher(lastPart);
        String ver;
        if (matcher.matches()){
            ver = matcher.group(1);
        }
        else{
            lastPart = spl[spl.length - 2];
            matcher = mtc.matcher(lastPart);
            ver = matcher.matches() ? matcher.group(1) : "1.0";
        }

        this.version = calculateVersionInteger(ver);

        base = base.replace(":" + lastPart, "");
    }

    private static int calculateVersionInteger(String version){
        var spl = version.split("\\.");
        int sum = 0;
        for (int i = 1; i <= spl.length; i++){
            var val = spl[i - 1];
            if (val == null || val.isBlank())
                continue;
            sum += (int) (Math.pow(10, spl.length - i) * Integer.parseInt(val));
        }

        return sum;
    }

    public Asset(String path, String url){
        this.url = url;
        this.path = path;
        size = -1;
    }

    public boolean isLegacy(){
        return id.equals("legacy");
    }

    public boolean isVeryLegacy(){
        return id.equals("pre-1.6");
    }
}
