package com.laeben.corelauncher.ui.util;

import com.laeben.corelauncher.ui.entity.EventFilter;

import java.util.ArrayList;
import java.util.List;

public class EventFilterManager {
    private final List<EventFilter> filters;

    public EventFilterManager() {
        this.filters = new ArrayList<>();
    }

    public void addEventFilter(EventFilter filter){
        filter.add();
        filters.add(filter);
    }

    public void removeEventFilter(Object target){
        var lst = filters.stream().filter(a -> a.checkOwner(target)).toList();
        for (var f : lst){
            f.remove();
            filters.remove(f);
        }
    }

    public void clear(){
        for (var filter : filters){
            filter.remove();
        }

        filters.clear();
    }
}
