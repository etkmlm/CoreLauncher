package com.laeben.corelauncher.api.gpu;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.gpu.entity.GPUDisplay;
import com.laeben.corelauncher.api.gpu.entity.GPUType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinuxGPUSelector {
    private static final Path DRM_PATH = Path.begin(java.nio.file.Path.of("/sys/class/drm"));

    private static final List<GPUDisplay> DEFAULT_DISPLAYS = List.of(
        new GPUDisplay(null, new GPUType("0")), // primary
        new GPUDisplay(null, new GPUType("1")) // secondary
    );

    private static String parseVendorName(String vendorId) {
        if (vendorId == null) return "???";
        return switch (vendorId.toLowerCase()) {
            case "0x10de" -> "NVIDIA";
            case "0x1002" -> "AMD";
            case "0x8086" -> "Intel";
            default -> vendorId;
        };
    }

    public static List<GPUDisplay> getGPUDisplays(){
        var list = new ArrayList<GPUDisplay>();

        if (!DRM_PATH.exists()) return DEFAULT_DISPLAYS;

        for(var f : DRM_PATH.getFiles()){
            if (!f.getName().startsWith("renderD")) continue;

            final Path deviceFolder = f.to("device");
            java.nio.file.Path deviceLink;

            try {
                deviceLink = deviceFolder.toFile().toPath().toRealPath();
            } catch (IOException e) {
                continue;
            }

            final String pciAddress = deviceLink.getFileName().toString();
            final String driPrimeValue = "pci-" + pciAddress.replace(":", "_").replace(".", "_");

            String deviceId = deviceFolder.to("device").read();
            String vendorId = deviceFolder.to("vendor").read();
            if (deviceId != null) deviceId = deviceId.trim();
            if (vendorId != null) vendorId = vendorId.trim();

            list.add(new GPUDisplay(String.format("%s (id: %s)", parseVendorName(vendorId), deviceId), new GPUType(driPrimeValue)));
        }

        return Collections.unmodifiableList(list);
    }
}
