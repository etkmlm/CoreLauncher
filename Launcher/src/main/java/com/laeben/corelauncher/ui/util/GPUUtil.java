package com.laeben.corelauncher.ui.util;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.gpu.LinuxGPUSelector;
import com.laeben.corelauncher.api.gpu.WindowsGPUSelector;
import com.laeben.corelauncher.api.gpu.entity.GPUDisplay;

import java.util.List;

public class GPUUtil {
    public static String getGPUName(GPUDisplay display){
        if (display.displayName() == null && display.type().id() == null)
            return Translator.translate("gpu.default");

        if (display.displayName() == null)
            return Translator.translate("gpu." + OS.getSystemOS().getName() + "." + display.type().id());

        return display.displayName();
    }

    public static List<GPUDisplay> getAllGPUTypes(){
        return switch (OS.getSystemOS()){
            case WINDOWS -> WindowsGPUSelector.getGPUDisplays();
            case LINUX -> LinuxGPUSelector.getGPUDisplays();
            default -> List.of();
        };
    }
}
