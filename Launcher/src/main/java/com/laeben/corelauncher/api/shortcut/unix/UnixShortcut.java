package com.laeben.corelauncher.api.shortcut.unix;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.shortcut.Shortcut;

import java.io.IOException;

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
    public void create(Path shortcutPath, Path targetPath, Path workingDirectory, Path iconPath, String arguments) throws StopException, IOException {
        var write = String.format(TEMPLATE, shortcutPath.getNameWithoutExtension(), workingDirectory, targetPath.toString().replace(" ", "\\s"), arguments, iconPath);
        shortcutPath.write(write);
    }
}
