package com.laeben.corelauncher.api.shortcut.unix;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.shortcut.Shortcut;

public class UnixShortcut implements Shortcut {
    private static final UnixShortcut INSTANCE = new UnixShortcut();

    private static final String TEMPLATE =
            """
            [Desktop Entry]
            Name=%s
            Terminal=false
            Path=%s
            Exec="%s" %s
            NoDisplay=true
            Type=Application
            StartupNotify=true
            Icon=%s
            """;

    public static UnixShortcut getInstance() {
        return INSTANCE;
    }

    @Override
    public void create(Path shortcutPath, Path targetPath, Path workingDirectory, Path iconPath, String arguments) {
        var write = String.format(TEMPLATE, shortcutPath.getNameWithoutExtension(), workingDirectory, targetPath.toString().replace(" ", "\\s"), arguments, iconPath);
        shortcutPath.write(write);
    }
}
