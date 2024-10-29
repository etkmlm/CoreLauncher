package com.laeben.corelauncher.api.shortcut;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.shortcut.osx.OSXShortcut;
import com.laeben.corelauncher.api.shortcut.unix.UnixShortcut;
import com.laeben.corelauncher.api.shortcut.windows.WindowsShortcut;

public interface Shortcut {
    static void create(Path shortcutPath, Path targetPath, Path workingDirectory, Path iconPath, String arguments, OS os){
        switch (os){
            case WINDOWS -> WindowsShortcut.getInstance().create(shortcutPath, targetPath, workingDirectory, iconPath, arguments);
            case LINUX -> UnixShortcut.getInstance().create(shortcutPath, targetPath, workingDirectory, iconPath, arguments);
            case OSX -> OSXShortcut.getInstance().create(shortcutPath, targetPath, workingDirectory, iconPath, arguments);
            default -> {

            }
        }
    }

    static String getExtension(OS os){
        return switch (os){
            case WINDOWS -> ".lnk";
            case OSX -> ".command";
            default -> ".desktop";
        };
    }

    void create(Path shortcutPath, Path targetPath, Path workingDirectory, Path iconPath, String arguments);
}
