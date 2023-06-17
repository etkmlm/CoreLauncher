package com.cdev.clfixer;

import java.lang.instrument.Instrumentation;

public class Agent {
    public static void premain(String args, Instrumentation instrumentation){
        instrumentation.addTransformer(new Transformer());
    }

    public static void premain(String args) {

    }
}
