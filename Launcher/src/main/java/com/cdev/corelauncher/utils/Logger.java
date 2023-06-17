package com.cdev.corelauncher.utils;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.utils.entities.LogType;
import com.cdev.corelauncher.utils.entities.Path;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class Logger {
    private static Logger instance;

    private Path logDir;

    public Logger(){
        logDir = logDir();

        Configurator.getConfigurator().getHandler().addHandler("logger", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;

            logDir = logDir();

        });

        instance = this;
    }

    public static Path logDir(){
        return Configurator.getConfig().getLauncherPath().to("logs");
    }

    public static Logger getLogger(){
        return instance;
    }

    public void setLogDir(Path logDir){
        this.logDir = logDir;
    }

    private Path todayLogFile(){
        var today = Calendar.getInstance();
        String name = String.format("%02d", today.get(Calendar.DAY_OF_MONTH)) + "." + String.format("%02d", today.get(Calendar.MONTH)) + "." + today.get(Calendar.YEAR) + ".log";
        return logDir.to(name);
    }

    private String generate(LogType type, String content){
        return "[" + type + "] " + content;
    }

    public void printLog(LogType type, String content){
        System.out.println(generate(type, content));
    }

    public void log(LogType type, String content){
        String message = generate(type, content);
        System.out.println(message);
        if (Configurator.getConfig().getLogMode())
            todayLogFile().append("\n" + message);
    }

    public void log(Exception e){
        var w = new StringWriter();
        var p = new PrintWriter(w);
        e.printStackTrace(p);
        log(LogType.ERROR, w.toString());
    }
}
