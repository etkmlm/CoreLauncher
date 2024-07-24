package com.laeben.corelauncher.minecraft;

import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.core.entity.Path;

import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Session {
    private final Thread inputThread;
    private final Thread errorThread;

    private boolean stopRequested;
    private BufferedReader inputReader;
    private BufferedReader errorReader;
    private final List<String> commands;
    private final Path workDir;
    private final Path logFile;
    private static int lastSessionId = 0;
    private final int sessionId;

    private int exitCode;

    public Session(Path workDir, List<String> commands){
        this.commands = commands;
        this.workDir = workDir;

        inputThread = new Thread(() -> reader(inputReader, "IN"));
        errorThread = new Thread(() -> reader(errorReader, "ERROR"));

        sessionId = lastSessionId++;
        var dtt = LocalDateTime.now(ZoneId.systemDefault());
        var formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm");
        String dt = formatter.format(dtt);
        logFile = Configurator.getConfig().getLauncherPath().to("gamelog", dt + " - S" + sessionId + ".log");
    }

    public int getSessionId(){
        return sessionId;
    }

    public int getExitCode(){
        return exitCode;
    }

    public Session start(){
        stopRequested = false;

        try{
            Logger.getLogger().logHyph("SESSION START: " + sessionId);
            var process = new ProcessBuilder()
                    .directory(workDir.toFile())
                    .command(commands)
                    .start();

            inputReader = process.inputReader();
            errorReader = process.errorReader();

            inputThread.start();
            errorThread.start();

            String exit = "\n" + "EXIT CODE: " + (exitCode = process.waitFor());
            logFile.append(exit);
            Logger.getLogger().logDebug(exit);
            process.destroyForcibly();
        }
        catch (Exception e){
            Logger.getLogger().logHyph("SESSION ERROR: " + sessionId);
            Logger.getLogger().log(e);
        }

        Logger.getLogger().logHyph("SESSION END: " + sessionId);

        return this;
    }

    public void stop(){
        stopRequested = true;
    }

    private void log(String prefix, String line){
        String msg = "[S" + sessionId + prefix + "] " + line;
        System.out.println(msg);
        logFile.append("\n" + msg);
    }

    private void reader(BufferedReader reader, String prefix){
        String read;
        while (true){
            try{
                if (stopRequested || (read = reader.readLine()) == null)
                    break;
                log(prefix, read);
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }
    }


}
