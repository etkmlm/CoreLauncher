package com.laeben.corelauncher.api.shortcut.windows.shell;

import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.IOException;

public class ShellStringData implements ByteWriter.Serializable{
    private String name;
    private String relativePath;
    private String workingDir;

    private String commandLineArguments;
    private String iconLocation;

    public ShellStringData setName(String name) {
        this.name = name;

        return this;
    }

    public ShellStringData setRelativePath(String relativePath) {
        this.relativePath = relativePath;

        return this;
    }

    public ShellStringData setWorkingDir(String workingDir) {
        this.workingDir = workingDir;

        return this;
    }

    public ShellStringData setCommandLineArguments(String commandLineArguments) {
        this.commandLineArguments = commandLineArguments;

        return this;
    }

    public ShellStringData setIconLocation(String iconLocation) {
        this.iconLocation = iconLocation;

        return this;
    }

    @Override
    public int serialize(ByteWriter writer) throws IOException {
        int size = 0;

        if (name != null)
            size += writer.writeStringWithLength(name);
        if (relativePath != null)
            size += writer.writeStringWithLength(relativePath);
        if (workingDir != null)
            size += writer.writeStringWithLength(workingDir);
        if (commandLineArguments != null)
            size += writer.writeStringWithLength(commandLineArguments);
        if (iconLocation != null)
            size += writer.writeStringWithLength(iconLocation);

        return size;
    }
}