package com.laeben.corelauncher.api.concurrency;

import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.corelauncher.api.ui.UI;

import java.util.Timer;
import java.util.TimerTask;

public class ProgressTimer implements ProgressFunction {
    private final int interval;
    private final Timer timer;
    private final TimerTask task;

    private boolean stopped;

    private boolean pendingTask;
    private long current = 0;
    private long total = 0;
    private EventContext context = null;

    private ProgressFunction onTick;

    public ProgressTimer(int interval) {
        this.interval = interval;
        this.timer = new Timer();
        this.task = new TimerTask() {
            @Override
            public void run() {
                if (!pendingTask) return;
                if (onTick != null) UI.runAsync(() -> onTick.onProgress(current, total, context));
                pendingTask = false;
            }
        };
    }

    @Override
    public void onProgress(long current, long total, EventContext context){
        this.pendingTask = true;
        this.current = current;
        this.total = total;
        this.context = context;
    }

    public void triggerLatestTick(){
        UI.runAsync(() -> {
            if (onTick != null) onTick.onProgress(current, total, context);
        });
    }

    public void setOnTick(ProgressFunction onTick) {
        this.onTick = onTick;
    }

    public void start(){
        if (stopped) return;

        this.timer.scheduleAtFixedRate(task, 0, interval);
    }

    public void stop(){
        if (stopped) return;

        this.pendingTask = false;
        this.timer.cancel();
        stopped = true;
    }
}
