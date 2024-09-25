package com.laeben.clpatcher;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.JarURLConnection;
import java.util.jar.JarFile;

public class Agent {

    public static void premain(String args, Instrumentation instrumentation){
        JarFile file;
        try {
            JarURLConnection connection = (JarURLConnection)Agent.class.getResource("Agent.class").openConnection();
            file = connection.getJarFile();
            instrumentation.appendToSystemClassLoaderSearch(file);
            instrumentation.appendToBootstrapClassLoaderSearch(file);
            //System.out.println(file.getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        instrumentation.addTransformer(new Transformer(System.getProperty("com.laeben.clpatcher.args"), file.getName()));
    }

    public static void premain(String args) {

    }
}
