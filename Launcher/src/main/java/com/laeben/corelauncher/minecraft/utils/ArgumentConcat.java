package com.laeben.corelauncher.minecraft.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArgumentConcat {
    public static class Arg{

        private String key;
        private String value;

        public Arg(String key){
            this.key = key;
        }

        public Arg setValue(String value){
            this.value = value;

            return this;
        }

        public boolean isExports(){
            return key.equals("--add-exports");
        }

        public boolean isOpens(){
            return key.equals("--add-opens");
        }

        public boolean isModules(){
            return key.equals("--add-modules");
        }

        @Override
        public boolean equals(final Object obj){
            return obj instanceof Arg arg && key.equals(arg.key);
        }

        @Override
        public int hashCode(){
            return -1;
        }
    }

    private List<Arg> args;

    public ArgumentConcat(List<String> argz){
        args = new ArrayList<>();

        for (int i = 0; i < argz.size(); i++){
            String a = argz.get(i);
            if (a.startsWith("--add-")){
                args.add(new Arg(a + (a.contains("=") ? "" : "=" + argz.get(++i))));
            }
            else if (a.startsWith("--")){
                var arg = new Arg(a).setValue(argz.get(++i));
                if (arg.isOpens() || arg.isExports() || arg.isModules())
                    args.add(0, arg);
                else
                    args.add(arg);
            }
            else if (a.startsWith("-") && !a.startsWith("-D") && !a.startsWith("-X"))
                args.add(new Arg(a).setValue(argz.get(++i)));
            else if (a.isEmpty() || a.isBlank()){

            }
            else
                args.add(new Arg(a));
        }
    }

    public ArgumentConcat disable(String key){
        args.removeIf(x -> x.key.startsWith(key));
        return this;
    }

    public ArgumentConcat concat(ArgumentConcat argg){
        args.addAll(argg.args);
        args = args.stream().distinct().collect(Collectors.toList());
        return this;
    }

    public ArgumentConcat register(String p, String value){
        if (value == null){
            args.removeIf(x -> x.key.contains(p) || (x.value != null && x.value.contains(p)));
            return this;
        }
        args.forEach(x -> {
            if (x.value != null)
                x.value = x.value.replace(p, value);
            else
                x.key = x.key.replace(p, value);
        });
        return this;
    }

    public String[] build(){
        return args.stream().map(x -> new String[] {x. key, x.value}).flatMap(Arrays::stream).filter(Objects::nonNull).toArray(String[]::new);
    }

}
