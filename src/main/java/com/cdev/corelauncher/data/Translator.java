package com.cdev.corelauncher.data;

import com.cdev.corelauncher.data.entities.Language;
import com.cdev.corelauncher.data.entities.Translate;
import com.google.gson.*;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Translator {

    private static Translator instance;

    private List<Language> languages;
    private List<Translate> translates;

    private Language selectedLanguage;

    public Translator(){
        selectedLanguage = Language.fromKey("EN");

        instance = this;
    }

    public Language findLanguage(String key){
        return languages.stream().filter(x -> x.getKey().equals(key)).findFirst().orElse(Language.fromKey(key));
    }

    public List<Language> getAllLanguages(){
        return languages;
    }

    public void setLanguage(Language selectedLanguage){
        this.selectedLanguage = selectedLanguage;
    }

    public String getTranslate(String key){
        return translates.stream().filter(x -> x.check(selectedLanguage, key)).findFirst().orElse(Translate.empty()).getValue();
    }

    public static Translator generateTranslator(){

        var gson = new GsonBuilder().registerTypeAdapter(Translator.class, (JsonDeserializer<Translator>) (jsonElement, type, jsonDeserializationContext) -> {
            var obj = jsonElement.getAsJsonObject();
            Translator t = new Translator();
            t.languages = obj.get("languages").getAsJsonArray().asList().stream().map(x -> (Language)jsonDeserializationContext.deserialize(x, Language.class)).toList();
            t.translates = new ArrayList<>();

            for(var s : obj.get("translates").getAsJsonObject().entrySet()){

                String key = s.getKey();

                var val = s.getValue().getAsJsonObject();
                for(var l : val.entrySet()){
                    t.translates.add(new Translate(key, t.findLanguage(l.getKey()), l.getValue().getAsString()));
                }
            }

            return t;
        }).create();

        var s = Translator.class.getResourceAsStream("/com/cdev/corelauncher/json/translate.json");
        if (s == null)
            throw new RuntimeException("Translate file can't found.");

        return gson.fromJson(new InputStreamReader(s), Translator.class);
    }

    public static Translator getTranslator(){
        return instance;
    }
}
