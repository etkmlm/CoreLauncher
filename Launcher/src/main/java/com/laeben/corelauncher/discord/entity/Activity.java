package com.laeben.corelauncher.discord.entity;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.socket.entity.CLStatusPacket;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Activity {

    public enum Type{
        PLAYING, STREAMING, LISTENING, WATCHING, CUSTOM, COMPETITING
    }

    public static class Timestamps{
        public Integer start;
        public Integer end;

        public Timestamps(int start, int end){
            this.start = start;
            this.end = end;
        }

        public Timestamps(int start){
            this.start = start;
        }

        public static Timestamps now(){
            return new Timestamps((int)(System.currentTimeMillis() / 1000));
        }
    }

    public static class Assets{
        public String large_image;
        public String large_text;
        public String small_image;
        public String small_text;

        public Assets setLarge(String image, String text){
            this.large_image = image;
            this.large_text = text;
            return this;
        }

        public Assets setSmall(String image, String text){
            this.small_image = image;
            this.small_text = text;
            return this;
        }
    }

    public static class Party{
        public String id;
        public int[] size;

        public Party(String id, int current, int max){
            this.id = id;
            this.size = new int[] {current, max};
        }
    }

    public static class Secrets{
        public String join;
        public String spectate;
        public String match;

        public static Secrets joinSecrets(String join){
            var s = new Secrets();
            s.join = join;
            return s;
        }
    }

    public static class Button{
        public String label;
        public String url;

        public Button(String label, String url){
            this.label = label;
            this.url = url;
        }
    }

    public int type;
    public String name;
    public String state;
    public String details;
    public Timestamps timestamps;
    public Assets assets;
    public Party party;
    public Secrets secrets;
    public List<Button> buttons;
    public boolean instance;

    public static Consumer<Activity> setForProfile(Profile p){
        return a -> {
            a.timestamps = Activity.Timestamps.now();
            var assets = new Activity.Assets().setLarge("mc512", p.getVersionId());
            if (!p.getLoader().getType().isNative()){
                var ident = p.getLoader().getType().getIdentifier();
                assets.setSmall(ident, ident + " " + p.getLoaderVersion());
            }
            a.buttons = null;
            a.party = null;
            a.assets = assets;
            a.details = Translator.translateFormat("discord.detail.play", p.getName());
            a.state = "Minecraft " + p.getLoader().getType().getIdentifier() + " " + p.getVersionId();
        };
    }

    public static Consumer<Activity> setForIdling(){
        return a -> {
            a.type = Type.PLAYING.ordinal();
            a.state = Translator.translate("discord.state.idle");;
            a.details = Translator.translate("discord.detail.surf");
            a.timestamps = null;
            a.assets = null;
            a.party = null;
            a.buttons = null;
        };
    }

    private static final Pattern privateV4Pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+(:\\d+)?");

    public static Consumer<Activity> setForInGame(CLStatusPacket pack){
        return a -> {
            a.type = Type.PLAYING.ordinal();
            a.timestamps = Activity.Timestamps.now();
            a.details = Translator.translate("discord.detail." + pack.getType().name().toLowerCase(Locale.US));
            a.state = pack.getData() != null && privateV4Pattern.matcher(pack.getData()).matches() ? Translator.translate("discord.state.private") : pack.getData();
            if (pack.getType() != CLStatusPacket.InGameType.MULTIPLAYER){
                a.party = null;
                a.buttons = null;
            }
            else{
                a.buttons = List.of(new Button(Translator.translate("discord.button.join"), "http://localhost:9845/join?server=" + pack.getData()));
            }
        };
    }

    public static Consumer<Activity> setForParty(String id, int current, int max){
        return a -> a.party = new Party(id, current, max);
    }
}
