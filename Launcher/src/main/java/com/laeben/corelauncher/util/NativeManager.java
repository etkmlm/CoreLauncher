package com.laeben.corelauncher.util;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.api.util.entity.NetParcel;
import com.laeben.corelauncher.util.entity.LogType;

public class NativeManager {
    private static final String LINUX_EXT = "so";
    private static final String WIN_EXT = "dll";
    private static final String MAC_EXT = "dynlib";
    private static String getExtension(){
        return switch (OS.getSystemOS()){
            case WINDOWS -> WIN_EXT;
            case OSX -> MAC_EXT;
            default -> LINUX_EXT;
        };
    }

    private static String getModuleURL(String pkg, String module, String version, String os){
        return "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-%s.jar".formatted(pkg, module, version, module, version, os);
    }

    public static Path getNativePath(String key){
        return Configurator.getConfig().getNativesPath().to(key + "." + getExtension());
    }

    /**
     * Downloads and extracts the native libraries into the natives folder.
     * @param pkg ex: org/openjfx
     * @param module ex: javafx-controls
     * @param version ex: 21.0.7
     */
    public static void downloadModule(String pkg, String module, String version) throws NoConnectionException, HttpException, StopException {
        final String extension = getExtension();

        final Path folder = Configurator.getConfig().getNativesPath();
        final Path jarPath = Configurator.getConfig().getTemporaryFolder().to(module + version + ".jar");
        final Path exDir = Configurator.getConfig().getTemporaryFolder().to(module + version);

        String os = switch (OS.getSystemOS()){
            case WINDOWS -> "win";
            case OSX -> {
                if ((CoreLauncher.SYSTEM_OS_ARCH.equals("aarch64") || CoreLauncher.SYSTEM_OS_ARCH.equals("arm64")))
                    yield "mac";
                else yield "mac-aarch64";
            }
            default -> "linux";
        };

        String url = getModuleURL(pkg, module, version, os);

        Logger.getLogger().log(LogType.INFO, "Downloading module %s from package %s with version %s, URL = '%s'".formatted(module, pkg, version, url));

        var parcel = NetParcel.create(url, jarPath, false);
        var path = NetUtil.download(parcel);
        path.extract(exDir, null);

        for(var file : exDir.getFiles()){
            if (!extension.equals(file.getExtension()))
                continue;

            file.move(folder.to(file.getName()));
        }

        exDir.delete();
        path.delete();

        Logger.getLogger().log(LogType.INFO, "Successfully downloaded module %s from package %s with version %s".formatted(module, pkg, version));
    }
}
