package com.laeben.corelauncher.api.shortcut.windows.shell;

import com.laeben.corelauncher.api.shortcut.windows.shell.item.ShellItem;
import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShellTargetIDList implements ByteWriter.Serializable {
    private final List<ShellItem> items;

    private ShellTargetIDList(){
        items = new ArrayList<>();
    }

    public static ShellTargetIDList create(){
        return new ShellTargetIDList();
    }

    public ShellTargetIDList add(ShellItem item){
        items.add(item);

        return this;
    }

    @Override
    public int serialize(ByteWriter writer) throws IOException {
        short size = 2; // only for size variable, terminal is not included
        var list = new ArrayList<byte[]>();

        try(var stream = new ByteArrayOutputStream();
            var wr = new ByteWriter(stream)){
            for (var i : items){
                size += (short) i.serialize(wr);
                list.add(stream.toByteArray());
                stream.reset();
            }
        }
        writer.writeShort(size);
        for (var i : list)
            writer.write(i);
        writer.writeShort(0);

        return size;
    }
}