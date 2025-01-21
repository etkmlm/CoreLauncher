package com.laeben.corelauncher.util;

import java.util.Arrays;
import java.util.function.Consumer;

// Needs To Save! Manager
public class NTSManager {
    private final boolean[] ntsList;
    private Consumer<Integer> onSet;

    private boolean nts;

    public NTSManager(int size) {
        ntsList = new boolean[size];
    }

    public boolean calcNts(){
        nts = false;
        for (boolean b : ntsList) {
            if (b) {
                nts = true;
                break;
            }
        }
        return nts;
    }

    public boolean needsToSave(){
        return nts;
    }

    public void setOnSet(Consumer<Integer> onSet) {
        this.onSet = onSet;
    }

    public void set(int i, boolean v){
        ntsList[i] = v;

        if (onSet != null)
            onSet.accept(i);
    }

    public void clear(){
        Arrays.fill(ntsList, false);
        if (onSet != null)
            onSet.accept(-1);
    }
}
