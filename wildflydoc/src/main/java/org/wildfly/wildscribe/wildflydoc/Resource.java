package org.wildfly.wildscribe.wildflydoc;

import static java.util.Collections.emptyList;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

record Capability(String name, boolean dynamic) {

}

record Resource(String description, String storage, List<Child> children) {
    public static Resource fromModelNode(PathAddress pathAddress, final ModelNode node, Map<String, Capability> capabilities) {
        final List<Child> children = new ArrayList<>();
        if (node.hasDefined("children")) {
            for (Property property : node.get("children").asPropertyList()) {
                children.add(Child.fromProperty(property));
            }
            Collections.sort(children);
        }

        String storage = node.get("storage").asString("configuration");

        return new Resource(node.get("description").asString(), storage, children);
    }
}

record Child(String name, String description, Deprecated deprecated, List<Child> children) implements Comparable<Child>{
    public static Child fromProperty(final Property property) {
        String name = property.getName();
        String description = property.getValue().get("description").asString();

        final List<Child> registrations = new ArrayList<Child>();
        ModelNode modelDesc = property.getValue().get("model-description");
        if (modelDesc.isDefined()) {
            for (Property child : modelDesc.asPropertyList()) {
                if (!child.getName().equals("*")) {
                    registrations.add(new Child(child.getName(), child.getValue().get("description").asString(""), Deprecated.fromModel(child.getValue()), emptyList()));
                }
            }
        }
        Collections.sort(registrations);

        Child op = new Child(name, description, Deprecated.fromModel(property.getValue()), registrations);

        return op;
    }

    @Override
    public int compareTo(Child o) {
        return name.compareTo(o.name);
    }
}

record Deprecated(boolean deprecated, String reason, String since) {
    public static Deprecated fromModel(final ModelNode model) {
        final boolean deprecated = model.hasDefined("deprecated");
        final String reason;
        final String since;
        if (deprecated) {
            final ModelNode dep = model.get("deprecated");
            reason = dep.get("reason").asString();
            since = dep.get("since").asString();
        } else {
            reason = null;
            since = null;
        }
        return new Deprecated(deprecated, reason, since);
    }
}