package com.laeben.corelauncher.minecraft.modding.modrinth.entity;

import com.laeben.core.entity.RequestParameter;
import com.laeben.core.util.StrUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SearchRinth {
    public String query;
    public String index;
    public FacetBuilder facets;
    public int offset;
    public int limit;

    public void setIndex(Index ix){
        index = ix.getId();
    }

    public Index getIndex(){
        return Index.fromId(index);
    }

    public List<RequestParameter> getParams(){
        var list = new ArrayList<RequestParameter>();

        if (query != null)
            list.add(new RequestParameter("query", query));
        if (index != null)
            list.add(new RequestParameter("index", index));
        var build = facets.build();
        if (!build.isEmpty())
            list.add(new RequestParameter("facets", StrUtil.jsArray(build.stream().filter(Facet::isPresent).map(Facet::toString).toList())).markAsEscapable());
        list.add(new RequestParameter("offset", offset));
        if (limit != 0)
            list.add(new RequestParameter("limit", limit));

        return list;
    }

    public void setQuery(String q){
        query = URLEncoder.encode(q, StandardCharsets.UTF_8);
    }
}
