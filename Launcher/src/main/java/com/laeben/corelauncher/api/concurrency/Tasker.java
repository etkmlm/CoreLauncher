package com.laeben.corelauncher.api.concurrency;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.entity.Logger;
import org.apache.commons.lang3.NotImplementedException;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tasker {
    public static class TaskRecord{
        private final WeakReference<Thread> thread;
        private final CancellableToken<?> cancellableToken;
        private Runnable onFinished;
        public TaskRecord(Thread thread, CancellableToken<?> cancellableToken){
            this.thread = new WeakReference<>(thread);
            this.cancellableToken = cancellableToken;
        }

        public void stop(){
            final Thread got = thread.get();
            if (got != null) got.interrupt();
            if (cancellableToken != null) cancellableToken.stop();
        }

        public TaskRecord onFinished(Runnable onFinished){
            this.onFinished = onFinished;
            return this;
        }

        public void onFinished(){
            if (onFinished != null) onFinished.run();
        }

        public boolean isRunning(){
            final Thread got = thread.get();
            return got != null && !got.isInterrupted();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Thread t && this.thread.refersTo(t) || o instanceof TaskRecord r && r.thread.refersTo(this.thread.get());
        }

        @Override
        public int hashCode(){
            final var thread = this.thread.get();
            return thread == null ? super.hashCode() : thread.hashCode();
        }
    }

    private static final Tasker defaultTasker = new Tasker();
    public static Tasker getDefault(){
        return defaultTasker;
    }

    private final ExecutorService executor;
    private final Set<TaskRecord> tasks;

    public Tasker(){
        executor = Executors.newFixedThreadPool(5);
        tasks = Collections.synchronizedSet(new HashSet<>());
    }

    public void enqueueTask(Callable<Void> callable){
        throw new NotImplementedException();
    }

    public void stopAll(){
        for (TaskRecord task : tasks) {
            task.stop();
        }
    }

    private void removeTask(Thread thread){
        for (TaskRecord task : tasks) {
            if (task.thread.refersTo(thread)){
                task.onFinished();
                tasks.remove(task);
                break;
            }
        }
    }

    public TaskRecord await(Runnable runnable){
        return await(runnable, null);
    }

    public TaskRecord await(Runnable runnable, CancellableToken<?> token){
        return await(() -> {
            runnable.run();
            return null;
        }, token);
    }

    /**
     * Functions will behave synchronous inside another task created by the same tasker.
     * Otherwise, they will be executed as a separate thread.
     * @param runnable target runnable
     * @return the record of the parent task or the new record
     */
    public TaskRecord awaitNested(Runnable runnable){
        try {
            return awaitNested(() -> {
                runnable.run();
                return null;
            });
        } catch (Exception ignored) {

        }

        throw new RuntimeException("Runnable thrown an exception!");
    }

    /**
     * Functions will behave synchronous inside another task created by the same tasker.
     * Otherwise, they will be executed as a separate thread.
     * @param callable target callable
     * @return the record of the parent task or the new record
     * @throws Exception exception from the sync task
     */
    public TaskRecord awaitNested(Callable<Void> callable) throws Exception {
        final var current = Thread.currentThread();
        TaskRecord foundRecord = null;

        for (var r : tasks){
            if (r.thread.refersTo(current)) {
                foundRecord = r;
                break;
            }
        }

        if (foundRecord != null){
            callable.call();
            return foundRecord;
        }

        return await(callable, null);
    }

    public TaskRecord await(Callable<Void> callable, CancellableToken<?> token){
        var thread = new Thread(() -> {
            try{
                callable.call();
            }
            catch (StopException | InterruptedException ignored){

            } catch (Exception e) {
                Logger.getLogger().log("Exception in the thread " + Thread.currentThread().getName(), e);
            }
            finally {
                Logger.getLogger().logDebug("Thread " + Thread.currentThread().getName() + " stopped.");
                removeTask(Thread.currentThread());
            }
        });
        var record = new TaskRecord(thread, token);
        tasks.add(record);

        thread.start();

        return record;
    }

    public void dispose(){
        stopAll();
        tasks.clear();
    }

}
