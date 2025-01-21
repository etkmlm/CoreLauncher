package com.laeben.corelauncher.api.exception;

public class PerformException extends Exception{
    private Object value;

    public PerformException(String key) {
        super(key);
    }

    public PerformException(String key, Object value){
        super(key);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

}
