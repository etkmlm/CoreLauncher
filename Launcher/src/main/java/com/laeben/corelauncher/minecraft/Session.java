package com.laeben.corelauncher.minecraft;

import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.core.entity.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Session {
    private final Thread inputThread;
    private final Thread errorThread;

    private boolean stopRequested;
    private BufferedReader inReader;
    private BufferedReader errorReader;
    private final List<String> commands;
    private final Path workDir;
    private final Path logFile;
    private static int lastSessionId = 0;
    private final int sessionId;

    public Session(Path workDir, List<String> commands){
        this.commands = commands;
        this.workDir = workDir;

        inputThread = new Thread(this::input);
        errorThread = new Thread(this::error);

        sessionId = lastSessionId++;
        var dtt = LocalDateTime.now(ZoneId.systemDefault());
        var formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm");
        String dt = formatter.format(dtt);
        logFile = Configurator.getConfig().getLauncherPath().to("gamelog", dt + " - S" + sessionId + ".log");
    }

    public Session start(){
        stopRequested = false;

        try{
            Logger.getLogger().logHyph("SESSION START: " + sessionId);

            var process = new ProcessBuilder()
                    .directory(workDir.toFile())
                    .command(commands)
                    .start();

            inReader = process.inputReader();
            errorReader = process.errorReader();

            inputThread.start();
            errorThread.start();

            process.waitFor();
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

    private void input(){
        String read;
        while (true){
            try {
                if (stopRequested || (read = inReader.readLine()) == null) break;

                String msg = "[S" + sessionId + "IN] " + read;
                System.out.println(msg);
                logFile.append("\n" + msg);

            } catch (IOException ignored) {

            }

        }
    }

    private void error(){
        String read;
        while (true){
            try {
                if (stopRequested || (read = errorReader.readLine()) == null) break;

                String msg = "[S" + sessionId + "ERROR] " + read;
                System.out.println(msg);
                logFile.append("\n" + msg);

            } catch (IOException ignored) {

            }

        }
    }


}
