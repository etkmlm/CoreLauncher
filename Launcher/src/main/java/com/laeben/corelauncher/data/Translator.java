package com.laeben.corelauncher.data;

import java.util.*;
import java.util.stream.Collectors;

public class Translator {

    private static Translator instance;

    private List<ResourceBundle> bundles;
    private ResourceBundle bundle;

    public Translator(){
        instance = this;
    }

    public List<Locale> getAllLanguages(){
        return bundles.stream().map(ResourceBundle::getLocale).collect(Collectors.toList());
    }

    public void setLanguage(Locale selectedLanguage){
        bundle = bundles.stream().filter(x -> x.getLocale().toLanguageTag().equals(selectedLanguage.toLanguageTag())).findFirst().orElse(null);
    }

    public static String translate(String key){
        return getTranslator().getTranslate(key);
    }

    public static String translateFormat(String key, Object... args){
        return getTranslator().getTranslateFormat(key, Arrays.stream(args).toList());
    }

    public String getTranslate(String key){
        return bundle.containsKey(key) ? bundle.getString(key) : null;
    }

    public String getTranslateFormat(String key, List<Object> args){
        var translate = getTranslate(key);
        if (translate == null)
            return null;

        int i = 0;

        try{
            while (true){
                int index = translate.indexOf("%");
                if (index == -1)
                    break;
                else
                    translate = translate.replaceFirst("%", args.get(i++).toString());
            }
        }
        catch (Exception ignored){

        }

        return translate;
    }

    public static void generateTranslator(){
        Translator t = new Translator().reload();
        t.setLanguage(Configurator.getConfig().getLanguage());
    }

    public Translator reload(){
        bundles = new ArrayList<>();

        for (var l : Locale.getAvailableLocales()){
            try{
                var x = ResourceBundle.getBundle("com.laeben.corelauncher.data.Translate", l);
                bundles.add(x);
            }
            catch (MissingResourceException ignored){

            }
        }

        bundles = bundles.stream().distinct().toList();

        return this;
    }

    public ResourceBundle getBundle(){
        return bundle;
    }

    public static Translator getTranslator(){
        return instance;
    }
}
