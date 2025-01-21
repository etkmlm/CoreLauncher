package com.laeben.corelauncher.ui.tutorial.entity;

import com.laeben.core.entity.TranslationBundle;
import com.laeben.corelauncher.api.Configurator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Tutorial {

    private final List<Step> steps;

    private TranslationBundle title;
    private TranslationBundle description;

    private Tutorial(){
        steps = new ArrayList<>();
    }

    public static Tutorial withInstructs(String title, String description) {
        var t = new Tutorial();
        var lang = Configurator.getConfig().getLanguage().getLanguage().toLowerCase();

        var bundle1 = new TranslationBundle();
        bundle1.put(lang, title);
        var bundle2 = new TranslationBundle();
        bundle2.put(lang, description);
        t.title = bundle1;
        t.description = bundle2;
        return t;
    }

    public Tutorial withStep(Step step) {
        steps.add(step);
        return this;
    }

    public Tutorial withSteps(Step... steps) {
        this.steps.addAll(Arrays.asList(steps));
        return this;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public String getTitle(Locale locale) {
        return title == null ? null : title.get(locale);
    }

    public String getDescription(Locale locale) {
        return description == null ? null : description.get(locale);
    }

    public boolean hasLocale(Locale locale){
        boolean a = title == null || title.has(locale);
        boolean b = description == null || description.has(locale);
        boolean c = steps.isEmpty() || steps.stream().allMatch(x -> x.getDescription(locale) != null);
        return a && b && c;
    }
}
