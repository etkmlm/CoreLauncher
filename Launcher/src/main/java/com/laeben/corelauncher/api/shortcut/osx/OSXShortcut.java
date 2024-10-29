package com.laeben.corelauncher.api.shortcut.osx;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.shortcut.Shortcut;

public class OSXShortcut implements Shortcut {
    private static final OSXShortcut INSTANCE = new OSXShortcut();

    public static OSXShortcut getInstance(){
        return INSTANCE;
    }

    private static final String TEMPLATE =
            """
            #!/bin/bash
            cd "%s"
            
            "%s" %s
            """;

    @Override
    public void create(Path shortcutPath, Path targetPath, Path workingDirectory, Path iconPath, String arguments) {
        var write = String.format(TEMPLATE, workingDirectory, targetPath, arguments);
        shortcutPath.write(write);
    }
}
