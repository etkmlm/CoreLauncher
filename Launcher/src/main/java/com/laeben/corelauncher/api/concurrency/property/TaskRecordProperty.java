package com.laeben.corelauncher.api.concurrency.property;

import com.laeben.corelauncher.api.concurrency.TaskRecord;
import javafx.beans.property.ObjectPropertyBase;

public class TaskRecordProperty extends ObjectPropertyBase<TaskRecord> {
    private static final String NAME = "TaskRecordProperty";

    @Override
    public Object getBean() {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    private void onFinished(){
        super.set(null);
    }

    public void set(TaskRecord value){
        super.set(value.onFinished(this::onFinished));
    }

    public boolean isRunning(){
        return get() != null;
    }

    public void cancelIfRunning(){
        if (isRunning()) get().stop();
    }
}
