package com.laeben.corelauncher.ui.entity;

import java.util.List;

public record FilterPreset(String id, List<FilterSection> sections) {
    public FilterSection getSection(String id) {
        return sections.stream().filter(section -> section.getIdentifier().equals(id)).findFirst().orElse(null);
    }
}