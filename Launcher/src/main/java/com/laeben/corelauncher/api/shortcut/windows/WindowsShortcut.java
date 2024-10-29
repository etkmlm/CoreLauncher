package com.laeben.corelauncher.api.shortcut.windows;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.shortcut.Shortcut;
import com.laeben.corelauncher.api.shortcut.windows.entity.FileAttribute;
import com.laeben.corelauncher.api.shortcut.windows.shell.ShellLinkHeader;
import com.laeben.corelauncher.api.shortcut.windows.shell.ShellStringData;
import com.laeben.corelauncher.api.shortcut.windows.shell.ShellTargetIDList;
import com.laeben.corelauncher.api.shortcut.windows.shell.item.FileEntryShellItem;
import com.laeben.corelauncher.api.shortcut.windows.shell.item.RootFolderShellItem;
import com.laeben.corelauncher.api.shortcut.windows.shell.item.VolumeShellItem;
import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

public class WindowsShortcut implements Shortcut {

    private static final WindowsShortcut INSTANCE = new WindowsShortcut();

    public static WindowsShortcut getInstance(){
        return INSTANCE;
    }

    @Override
    public void create(Path shortcutPath, Path targetPath, Path workingDirectory, Path iconPath, String arguments) {
        if (shortcutPath.exists())
            shortcutPath.delete();

        try(var file = Files.newOutputStream(shortcutPath.toFile().toPath());
            var writer = new ByteWriter(file))
        {
            // header

            new ShellLinkHeader()
                    .setLinkFlags(ShellLinkHeader.Flags.HAS_LINK_TARGET_ID_LIST /* | ShellLinkHeader.Flags.HAS_RELATIVE_PATH*/ | ShellLinkHeader.Flags.HAS_WORKING_DIR | ShellLinkHeader.Flags.HAS_ARGUMENTS | ShellLinkHeader.Flags.IS_UNICODE | (iconPath != null ? ShellLinkHeader.Flags.HAS_ICON_LOCATION : 0))
                    .setAttributes(FileAttribute.ARCHIVE)
                    .setShowCmd(ShellLinkHeader.ConsoleWindow.SW_NORMAL)
                    .serialize(writer);

            // target id list

            var tid = ShellTargetIDList.create().add(RootFolderShellItem.create(RootFolderShellItem.RootFolder.MY_COMPUTER_FOLDER).setSortIndex(RootFolderShellItem.SortIndex.COMPUTER));

            var path = List.of(targetPath.toString().split("\\\\"));
            if (!path.get(0).endsWith(":"))
                throw new InvalidObjectException("Target path is not correct.");

            String letter = String.valueOf(path.get(0).charAt(0)).toUpperCase(Locale.US);
            tid.add(VolumeShellItem.create(letter));
            int size = path.size();
            for (int i = 1; i < size; i++){
                var e = path.get(i);
                if (i == size - 1 && e.contains(".")){
                    tid.add(FileEntryShellItem.create().setFileAttributes(FileAttribute.ARCHIVE).setFileName(e).setIndicator(FileEntryShellItem.Indicators.IS_FILE | FileEntryShellItem.Indicators.HAS_UNICODE));
                    continue;
                }

                tid.add(FileEntryShellItem.create().setFileAttributes(FileAttribute.DIRECTORY).setFileName(e).setIndicator(FileEntryShellItem.Indicators.IS_DIRECTORY | FileEntryShellItem.Indicators.HAS_UNICODE));
            }

            tid.serialize(writer);

            // link info

            /*ShellLinkInfo.create()
                    .setFlags(ShellLinkInfo.VOLUME_ID_AND_LOCAL_BASE_PATH)
                    .setLocalBasePath(targetPath.toString())
                    .setVolumeID(VolumeID.create(VolumeID.DriveType.FIXED))
                    .serialize(writer);*/

            // string data

            var strData = new ShellStringData()
                    //.setRelativePath(".\\test.txt")
                    .setCommandLineArguments(arguments)
                    .setWorkingDir(workingDirectory.toString());

            if (iconPath != null)
                strData.setIconLocation(iconPath.toString());

            strData.serialize(writer);

        } catch (IOException e) {
            Logger.getLogger().log(e);
        }
    }
}
