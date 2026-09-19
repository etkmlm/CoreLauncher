package com.laeben.corelauncher.api.concurrency;

import com.laeben.core.concurrency.CancellableToken;

import java.lang.ref.WeakReference;

public class TaskRecord {
    private final WeakReference<Thread> thread;
    private final WeakReference<Object> owner;
    private final CancellableToken<?> cancellableToken;
    private Runnable onFinished;

    public TaskRecord(Object owner, Thread thread, CancellableToken<?> cancellableToken) {
        this.owner = new WeakReference<>(owner);
        this.thread = new WeakReference<>(thread);
        this.cancellableToken = cancellableToken;
    }

    public void stop() {
        final Thread got = thread.get();
        if (got != null) got.interrupt();
        if (cancellableToken != null) cancellableToken.stop();
    }

    public TaskRecord onFinished(Runnable onFinished) {
        this.onFinished = onFinished;
        return this;
    }

    public Object getOwner(){
        return owner.get();
    }

    public void onFinished() {
        if (onFinished != null) onFinished.run();
    }

    public WeakReference<Thread> getThreadReference(){
        return thread;
    }

    public boolean isRunning() {
        final Thread got = thread.get();
        return got != null && !got.isInterrupted();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Thread t && this.thread.refersTo(t) || o instanceof TaskRecord r && r.thread.refersTo(this.thread.get());
    }

    @Override
    public int hashCode() {
        final var thread = this.thread.get();
        return thread == null ? super.hashCode() : thread.hashCode();
    }
}
