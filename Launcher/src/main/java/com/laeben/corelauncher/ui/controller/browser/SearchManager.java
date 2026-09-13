package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.corelauncher.api.concurrency.Tasker;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.ModSource;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.ui.controller.browser.cell.ResourceCellItem;
import com.laeben.corelauncher.ui.controller.browser.search.Search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SearchManager {
    private final List<Search> searches;
    private final List<Pattern> regex;

    public SearchManager() {
        searches = new ArrayList<>();
        regex = new ArrayList<>();
    }

    public void add(Profile p, ModSource.Type... types){
        for (var type : types){
            var s = type.getSource().getSearch(p);
            s.reset();
            searches.add(s);
            regex.add(Pattern.compile("([\\s]*)?%([\\s]*)?".replace("%", type.getId())));
        }
    }

    public List<ResourceCellItem> search(String query, ResourceType type, Search search, Tasker installationTasker){
        if (search != null){
            if (type == null)
                return null;
            search.setMainType(type);
            return search.search(query, installationTasker);
        }

        for (int i = 0; i < regex.size(); i++) {
            var mt = regex.get(i).matcher(query);
            if (!mt.find())
                continue;
            query = query.replace(mt.group(0), " ").trim();
            var n = searches.get(i);
            if (type != null)
                n.setMainType(type);
            return n.search(query, installationTasker);
        }

        return null;
    }
}
