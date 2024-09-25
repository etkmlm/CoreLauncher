package com.laeben.corelauncher.discord.entity;

public class Command {

    public static class Data{
        public Integer code;
        public String message;
    }

    public static class Arguments{
        public int pid;
        public Activity activity;
    }

    public String cmd;
    public Arguments args;
    public String nonce;
    public String evt;
    public Data data;

    public static Command createForActivity(Activity activity){
        Command cmd = new Command();
        cmd.cmd = "SET_ACTIVITY";
        var args = new Arguments();
        args.activity = activity;
        args.pid = (int)ProcessHandle.current().pid();
        cmd.args = args;
        return cmd;
    }

    public boolean isSuccessful(){
        return evt == null;
    }
}
