package com.laeben.corelauncher.utils;

import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.utils.entities.LogType;
import com.laeben.core.entity.Path;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class Logger {
    private static Logger instance;
    private static final String logLines = generateHyph(10);
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm");

    private Path logDir;

    public Logger(){
        logDir = logDir();

        Configurator.getConfigurator().getHandler().addHandler("logger", (a) -> {
            if (!a.getKey().equals("gamePathChange"))
                return;

            logDir = logDir();

        }, false);

        instance = this;
    }

    public static Path logDir(){
        return Configurator.getConfig().getLauncherPath().to("logs");
    }

    public static Logger getLogger(){
        return instance;
    }

    private Path todayLogFile(){
        var today = Calendar.getInstance();
        String name = String.format("%02d", today.get(Calendar.DAY_OF_MONTH)) + "." + String.format("%02d", today.get(Calendar.MONTH)) + "." + today.get(Calendar.YEAR) + ".log";
        return logDir.to(name);
    }

    private static String generateHyph(int count){
        return "-".repeat(count);
    }

    private static String beginBlock(String title, String inner){
        String b = beginTitle(title);

        return b + "\n" + inner + "\n" + generateHyph(b.length() + title.length()) + "\n\n";
    }

    private static String beginTitle(String title){
        return logLines + title + logLines;
    }

    private String generate(LogType type, String content){
        return "[" + type + "] " + content;
    }

    public void printLog(LogType type, String content){
        System.out.println(generate(type, content));
    }

    public void logHyphBlock(String title, String content){
        log(beginBlock(title, content));
    }

    public void logHyph(String t){
        log(beginTitle(t));
    }

    public void logHyphEnd(int len){
        log(generateHyph(len));
    }

    private String getTime(){
        return format.format(Date.from(Instant.now()));
    }

    private void log(String content){
        System.out.println(content);
        if (Configurator.getConfig().getLogMode())
            todayLogFile().append("\n[" + getTime() + "] " + content);
    }

    public void log(LogType type, String content){
        if (type == LogType.ERROR)
            logHyphBlock("OOPS, AN ERROR!", content);
        else
            log(generate(type, content));
    }

    public void log(Exception e){
        var w = new StringWriter();
        var p = new PrintWriter(w);
        e.printStackTrace(p);
        log(LogType.ERROR, w.toString());
    }
}
