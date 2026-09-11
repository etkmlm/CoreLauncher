package com.laeben.corelauncher.ui.control;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.ui.UI;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @param <T> result type
 * @param <H> status type
 */
public class CWorker<T, H> extends StackPane {

    private Consumer<CWorker<T, H>> onDone;
    private Consumer<CWorker<T, H>> doFinally;
    private Consumer<CWorker<T, H>> onFailed;
    private Consumer<CWorker<T, H>> onStatus;

    private Function<CWorker<T, H>, Task<T>> taskFactory;

    private Throwable e;
    private T value;
    private final ObjectProperty<H> status;

    private Executor executor;
    private CancellableToken<?> cancellableToken;
    private boolean isRunning;
    private Task<T> task;
    private final ProgressIndicator indicator;

    public CWorker(){
        indicator = new ProgressIndicator();
        indicator.setPrefWidth(30);
        indicator.setPrefHeight(30);
        ind(false);

        status = new SimpleObjectProperty<>();
        status.addListener(a -> {
            if (onStatus != null)
                onStatus.accept(this);
        });

        getChildren().add(indicator);
    }

    private void ind(boolean v){
        setVisible(v);
        setManaged(v);
    }

    public CWorker<T, H> begin(){
        e = null;
        value = null;
        onDone = null;
        onFailed = null;

        return this;
    }

    public Throwable getError(){
        return e;
    }

    public T getValue(){
        return value;
    }

    public H getStatus(){
        return status.get();
    }

    public void setTaskStatus(H val){
        status.set(val);
    }

    public CWorker<T, H> withExecutor(Executor executor){
        this.executor = executor;

        return this;
    }

    public CWorker<T, H> withToken(CancellableToken<?> token){
        if (this.cancellableToken != null && !this.cancellableToken.stopRequested())
            throw new RuntimeException("Cannot overwrite unused cancellable token.");

        this.cancellableToken = token;

        return this;
    }

    public CWorker<T, H> finallyDo(Consumer<CWorker<T, H>> r){
        this.doFinally = r;
        return this;
    }

    public CWorker<T, H> onDone(Consumer<CWorker<T, H>> r){
        this.onDone = r;
        return this;
    }

    public CWorker<T, H> onStatus(Consumer<CWorker<T, H>> r){
        this.onStatus = r;
        return this;
    }

    public CWorker<T, H> onFailed(Consumer<CWorker<T, H>> r){
        this.onFailed = r;
        return this;
    }

    private boolean reloadTask(){
        if (taskFactory == null)
            return false;

        var task = taskFactory.apply(this);
        task.setOnFailed(a -> {
            e = a.getSource().getException();
            UI.runAsync(() -> ind(false));
            if (!(e instanceof StopException) && onFailed != null)
                onFailed.accept(this);
            if (doFinally != null)
                doFinally.accept(this);

            isRunning = false;
        });
        task.setOnSucceeded(a -> {
            value = (T)a.getSource().getValue();
            UI.runAsync(() -> ind(false));
            if (onDone != null)
                onDone.accept(this);
            if (doFinally != null)
                doFinally.accept(this);

            isRunning = false;
        });

        this.task = task;
        return true;
    }

    public CWorker<T, H> withTask(final Function<CWorker<T, H>, Task<T>> taskFactory){
        this.taskFactory = taskFactory;

        return this;
    }

    public CancellableToken<?> getCancellableToken(){
        if (cancellableToken == null || cancellableToken.stopRequested())
            cancellableToken = new CancellableToken<>();
        return cancellableToken;
    }

    public boolean isRunning(){
        return isRunning;
    }

    public void run(){
        if (!reloadTask())
            return;
        UI.runAsync(() -> ind(true));
        if (executor == null)
            executor = Executors.newSingleThreadExecutor();
        isRunning = true;
        executor.execute(task);
    }
}
