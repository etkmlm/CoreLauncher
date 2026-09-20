package com.laeben.corelauncher.api.concurrency;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.type.SimpleEvent;
import com.laeben.corelauncher.api.concurrency.event.TaskContext;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.event.bus.FrequentEventBus;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tasker {
    private static final Tasker defaultTasker = new Tasker();
    public static Tasker getDefault(){
        return defaultTasker;
    }

    private final FrequentEventBus<TaskContext, SimpleEvent<TaskContext>> eventBus;
    private final ExecutorService executor;
    private final Set<TaskRecord> tasks;

    public Tasker(){
        executor = Executors.newFixedThreadPool(5);
        tasks = Collections.synchronizedSet(new HashSet<>());

        eventBus = new FrequentEventBus<>();
    }

    public Set<TaskRecord> getTasks(){
        return Collections.unmodifiableSet(tasks);
    }
    public boolean isEmpty(){
        return tasks.isEmpty();
    }
    public int count(){
        return tasks.size();
    }

    public FrequentEventBus<TaskContext, SimpleEvent<TaskContext>> getHandler(){
        return eventBus;
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
            if (task.getThreadReference().refersTo(thread)){
                task.onFinished();
                tasks.remove(task);
                eventBus.execute(new SimpleEvent<>(TaskContext.REMOVED).withSource(task));
                break;
            }
        }
    }

    public TaskRecord await(Runnable runnable){
        return await(runnable, null, null);
    }

    public TaskRecord await(Runnable runnable, Object owner, CancellableToken<?> token){
        return await(() -> {
            runnable.run();
            return null;
        }, owner, token);
    }

    /**
     * Functions will behave synchronous inside another task created by the same tasker.
     * Otherwise, they will be executed as a separate thread.
     * @param runnable target runnable
     * @return the record of the parent task or the new record
     */
    public TaskRecord awaitNested(Runnable runnable){
        return awaitNested(runnable, null);
    }

    /**
     * Functions will behave synchronous inside another task created by the same tasker.
     * Otherwise, they will be executed as a separate thread.
     * @param runnable target runnable
     * @param owner task owner
     * @return the record of the parent task or the new record
     */
    public TaskRecord awaitNested(Runnable runnable, Object owner){
        try {
            return awaitNested(() -> {
                runnable.run();
                return null;
            }, owner);
        } catch (Exception ignored) {

        }

        throw new RuntimeException("Runnable thrown an exception!");
    }

    /**
     * Functions will behave synchronous inside another task created by the same tasker.
     * Otherwise, they will be executed as a separate thread.
     * @param callable target callable
     * @param owner task owner
     * @return the record of the parent task or the new record
     * @throws Exception exception from the sync task
     */
    public TaskRecord awaitNested(Callable<Void> callable, Object owner) throws Exception {
        final var current = Thread.currentThread();
        TaskRecord foundRecord = null;

        for (var r : tasks){
            if (r.getThreadReference().refersTo(current)) {
                foundRecord = r;
                break;
            }
        }

        if (foundRecord != null){
            callable.call();
            return foundRecord;
        }

        return await(callable, owner, null);
    }

    /**
     * Functions will behave synchronous inside another task created by the same tasker.
     * Otherwise, they will be executed as a separate thread.
     * @param callable target callable
     * @return the record of the parent task or the new record
     * @throws Exception exception from the sync task
     */
    public TaskRecord awaitNested(Callable<Void> callable) throws Exception {
        return awaitNested(callable, null);
    }

    public TaskRecord await(Callable<Void> callable, Object owner, CancellableToken<?> token){
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
        var record = new TaskRecord(owner, thread, token);
        tasks.add(record);
        eventBus.execute(new SimpleEvent<>(TaskContext.ADDED).withSource(record));

        thread.start();

        return record;
    }

    public void dispose(){
        stopAll();
        tasks.clear();
    }

}
