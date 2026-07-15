package com.laeben.corelauncher.lan.handler;

public abstract class ProgressHandler<T> {
    /**
     * Executed on each progress has been achieved.
     * @param payload the payload
     * @param current current amount
     * @param total target amount
     * @return true if process should continue or false if stop requested
     */
    public abstract boolean onProgress(T payload, long current, long total);
}
