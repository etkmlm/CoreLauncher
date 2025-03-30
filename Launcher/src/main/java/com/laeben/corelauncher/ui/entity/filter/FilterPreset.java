package com.laeben.corelauncher.ui.entity.filter;

import javafx.scene.Node;

import java.util.List;

public class FilterPreset {

    private boolean enableEventListening = true;

    private final String id;
    private final List<FilterSection> sections;

    public FilterPreset(String id, List<FilterSection> sections){
        this.id = id;
        this.sections = sections;
    }

    public FilterSection getSection(String id) {
        return sections.stream().filter(section -> section.getIdentifier().equals(id)).findFirst().orElse(null);
    }

    public List<FilterSection> sections() {
        return sections.stream().filter(Node::isVisible).peek(a -> {
            if (!a.validateDefaults() && a.isSingleton())
                a.reselectChoices();
            //a.getType().getScheme().setNodeSelected();
        }).toList();
    }

    public String id() {
        return id;
    }

    public boolean isEnabledEventListening() {
        return enableEventListening;
    }

    public void setEnabledEventListening(boolean eel) {
        this.enableEventListening = eel;
    }

    public void dispose(){
        sections.forEach(a -> {
            a.setOnAction(null);
            a.resetChoices();
        });
    }
}