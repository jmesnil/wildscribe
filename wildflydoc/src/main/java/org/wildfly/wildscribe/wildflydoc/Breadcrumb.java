package org.wildfly.wildscribe.wildflydoc;

import java.util.ArrayList;
import java.util.List;

record Breadcrumb(String label, String url) {
    static List<Breadcrumb> build(PathElement[] path) {
        final List<Breadcrumb> crumbs = new ArrayList<>();
        crumbs.add(new Breadcrumb("home", "index.html"));
        StringBuilder currentUrl = new StringBuilder();
        for (PathElement i : path) {
            if (!currentUrl.toString().isEmpty()) {
                currentUrl.append("/");
            }
            currentUrl.append(i.getKey());
            if (!i.isWildcard()) {
                currentUrl.append("/").append(i.getValue());
            }
            final String label = i.getKey() + (i.isWildcard() ? "" : ("=" + i.getValue()));
            String url = currentUrl.toString();
            crumbs.add(new Breadcrumb(label, url + (url.isEmpty() ? "" : "/") + "index.html"));
        }
        return crumbs;
    }
}

