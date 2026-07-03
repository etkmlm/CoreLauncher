package com.laeben.corelauncher.api.gpu;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.gpu.entity.GPUDisplay;
import com.laeben.corelauncher.api.gpu.entity.GPUType;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WindowsGPUSelector {
    public record WindowsGPUPreference(Path path, GPUType type){}

    private static final String REG_PATH = "HKEY_CURRENT_USER\\Software\\Microsoft\\DirectX\\UserGpuPreferences";
    private static final String REG_TYPE = "REG_SZ";

    private static final List<GPUDisplay> DEFAULT_DISPLAYS = List.of(
        new GPUDisplay(null, new GPUType("0")), // auto
        new GPUDisplay(null, new GPUType("1")), // primary
        new GPUDisplay(null, new GPUType("2")) // high performance
    );

    public static List<WindowsGPUPreference> readPreferences() throws IOException, InterruptedException {
        var process = new ProcessBuilder()
                .command("reg", "query", REG_PATH, "/d", "/f", "GpuPreference=")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start();

        final ArrayList<WindowsGPUPreference> preferences = new ArrayList<>();

        try(var reader = process.inputReader()){
            String line;
            while ((line = reader.readLine()) != null){
                var spl = line.split("\\s+");
                if (spl.length != 4 || !spl[2].equals(REG_TYPE)) continue;
                Path path;
                try{
                    path = Path.begin(java.nio.file.Path.of(spl[1]));
                } catch (InvalidPathException ignored){
                    continue;
                }
                final int prefIndex = spl[3].indexOf("GpuPreference");
                if (prefIndex == -1) continue;
                int number = spl[3].charAt(prefIndex + 14) - '0';

                preferences.add(new WindowsGPUPreference(path, new GPUType(String.valueOf(number))));
            }
        }

        process.waitFor();

        return Collections.unmodifiableList(preferences);
    }

    public static boolean addPreference(Path path, GPUType type) throws IOException, InterruptedException {
        return new ProcessBuilder()
                .command("reg", "add", REG_PATH, "/v", path.toString(), "/t", REG_TYPE, "/d", "GpuPreference=" + type.getId(OS.WINDOWS) + ";", "/f")
                .start().waitFor() == 0;
    }
    public static void addPreference(WindowsGPUPreference preference) throws IOException, InterruptedException {
        addPreference(preference.path, preference.type);
    }

    public static List<GPUDisplay> getGPUDisplays(){
        Process process;
        try {
            process = new ProcessBuilder("cmd.exe", "/c",
                    "pnputil /enum-devices /class Display | findstr /C:\"Device Description:\"")
                    .start();
        } catch (IOException e) {
            Logger.getLogger().log(e);
            return DEFAULT_DISPLAYS;
        }

        final var list = new ArrayList<GPUDisplay>();

        try (var reader = process.inputReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":")) {
                    String gpuName = line.substring(line.indexOf(":") + 1).trim();

                    int number = 1; // primary

                    String lowerName = gpuName.toLowerCase();
                    if (lowerName.contains("nvidia") || lowerName.contains("geforce") ||
                            lowerName.contains("radeon") || lowerName.contains("rtx") || lowerName.contains("arc")) {

                        number = 2; // high performance
                    }

                    if (!gpuName.isEmpty()) {
                        list.add(new GPUDisplay(gpuName, new GPUType(String.valueOf(number))));
                    }
                }
            }
        } catch (IOException e) {
            Logger.getLogger().log(e);
            return DEFAULT_DISPLAYS;
        }

        return Collections.unmodifiableList(list);
    }
}
