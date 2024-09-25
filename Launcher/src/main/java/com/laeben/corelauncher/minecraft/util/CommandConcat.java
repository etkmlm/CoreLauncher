package com.laeben.corelauncher.minecraft.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandConcat {
    private final List<String> commands;

    public CommandConcat(){
        commands = new ArrayList<>();
    }

    public CommandConcat add(String... commands){
        if (commands != null && commands.length != 0 && Arrays.stream(commands).noneMatch(String::isEmpty))
            this.commands.addAll(List.of(commands));

        return this;
    }

    public CommandConcat add(int index, List<String> commands){
        if (commands != null && !commands.isEmpty() && commands.stream().noneMatch(String::isEmpty))
            this.commands.addAll(index, commands);

        return this;
    }

    public CommandConcat add(String command){
        if (command != null && !command.isBlank())
            this.commands.add(command);
        return this;
    }

    public List<String> generate(){
        return commands;
    }

}
