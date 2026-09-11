package com.laeben.corelauncher.api.shortcut.osx;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.shortcut.Shortcut;

import java.io.IOException;

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
    public void create(Path shortcutPath, Path targetPath, Path workingDirectory, Path iconPath, String arguments) throws StopException, IOException {
        var write = String.format(TEMPLATE, workingDirectory, targetPath, arguments);
        shortcutPath.write(write);
    }
}
