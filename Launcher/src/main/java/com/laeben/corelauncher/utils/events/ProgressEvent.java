package com.laeben.corelauncher.utils.events;

public class ProgressEvent extends BaseEvent {
    public String key;
    public double remain;
    public double total;

    public ProgressEvent(String key, double remain, double total) {
        this.key = key;
        this.remain = remain;
        this.total = total;
    }

    public double getRemain(){
        double x = key.equals("download") ? remain / 1024 / 1024 : remain;
        return Math.floor(x * 10) / 10;
    }

    public double getTotal(){
        double x = key.equals("download") ? total / 1024 / 1024 : total;
        return Math.floor(x * 10) / 10;
    }

    public double getProgress(){
        return Math.floor(remain / total * 100) / 100;
    }

    public double getProgressPercent(){
        return getProgress() * 100;
    }
}
