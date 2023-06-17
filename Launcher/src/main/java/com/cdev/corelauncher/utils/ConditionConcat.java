package com.cdev.corelauncher.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConditionConcat<T> {
    public static class Condition<T>{
        private Supplier<Boolean> condition;
        private Supplier<T> then;

        public static <T> Condition<T> ifHappens(Supplier<Boolean> condition){
            var c = new Condition<T>();
            c.condition = condition;
            return c;
        }

        public Condition<T> then(Supplier<T> then){
            this.then = then;
            return this;
        }

        protected void s(){

        }
    }
    private final List<Condition<T>> conditions;

    public ConditionConcat(){
        conditions = new ArrayList<>();
    }

    public ConditionConcat<T> addCondition(Condition<T> c){
        conditions.add(c);
        return this;
    }

    public T execute(){
        for(var c : conditions){
            if (c.condition.get())
                try{return c.then.get();} catch (Exception e){ return null; }
        }

        return null;
    }

}
