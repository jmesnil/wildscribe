package org.wildfly.wildscribe.wildflydoc;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

record Capability(String name, boolean dynamic) {

}

record ResourceNode(String name, List<ResourceNode> children,
                    List<String> attributes) implements Comparable<ResourceNode> {
    public static ResourceNode fromModelNode(String indent, String name, final ModelNode model) {
        ArrayList<String> attributes = new ArrayList<>();
        if (model.hasDefined("attributes")) {
            for (Property attribute : model.get("attributes").asPropertyList()) {
                attributes.add(attribute.getName());
            }
        }
        ArrayList<ResourceNode> children = new ArrayList<>();

        Collections.sort(children);
        if (model.hasDefined("children") && !model.get("children").keys().isEmpty()) {
            for (Property child : model.get("children").asPropertyList()) {
                children.add(ResourceNode.fromModelNode("  " + indent, child.getName(), child.getValue()));

            }
        } else {
            if (model.hasDefined("model-description")) {
                for (Property child : model.get("model-description").asPropertyList()) {
                    if (child.getName().equals("*")) {
                        return fromModelNode(indent, name, child.getValue());
                    }
                    children.add(ResourceNode.fromModelNode("  " + indent, child.getName(), child.getValue()));
                }
            }
        }
        return new ResourceNode(name, children, attributes);
    }

    @Override
    public int compareTo(ResourceNode o) {
        return name.compareTo(o.name);
    }
}

record Resource(String description, String storage, List<Child> children, List<Attribute> attributes) {
    public static Resource fromModelNode(PathAddress pathAddress, final ModelNode node, Map<String, Capability> capabilities) {
        final List<Child> children = new ArrayList<>();
        if (node.hasDefined("children")) {
            for (Property property : node.get("children").asPropertyList()) {
                children.add(Child.fromProperty(property));
            }
            Collections.sort(children);
        }
        final List<Attribute> attributes = new ArrayList<Attribute>();
        if (node.hasDefined("attributes")) {
            for (Property i : node.get("attributes").asPropertyList()) {
                attributes.add(Attribute.fromProperty(i));
            }
            Collections.sort(attributes);
        }
        String storage = node.get("storage").asString("configuration");

        return new Resource(node.get("description").asString(), storage, children, attributes);
    }
}

record Child(String name, String description, Deprecated deprecated,
             List<Child> children) implements Comparable<Child> {
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

record Attribute(String name, String description, String type, boolean nillable, boolean expressionsAllowed,
                 String defaultValue, String min, String max, String accessType, String storage,
                 Deprecated deprecated,
                 String unit, String restartRequired, String capabilityReference, String stability,
                 Collection<String> allowedValues) implements Comparable<Attribute> {
    public static Attribute fromProperty(final Property property) {
        String name = property.getName();
        String description = property.getValue().get("description").asString();
        String type = property.getValue().get("type").asString();
        boolean nilable = true;
        if (property.getValue().hasDefined("nillable")) {
            nilable = property.getValue().get("nillable").asBoolean();
        }
        String defaultValue = null;
        if (property.getValue().hasDefined("default")) {
            defaultValue = property.getValue().get("default").asString();
        }
        boolean expressionsAllowed = false;
        if (property.getValue().hasDefined("expressions-allowed")) {
            expressionsAllowed = property.getValue().get("expressions-allowed").asBoolean();
        }
        String min = null;
        if (property.getValue().hasDefined("min")) {
            min = property.getValue().get("min").asString();
        }
        String max = null;
        if (property.getValue().hasDefined("max")) {
            max = property.getValue().get("max").asString();
        }
        String accessType = property.getValue().get("access-type").asString();
        String storage = property.getValue().get("storage").asString();
        String unit = null;
        if (property.getValue().hasDefined("unit")) {
            unit = property.getValue().get("unit").asString();
        }
        String restartRequired = property.getValue().get("restart-required").asStringOrNull();
        String capabilityReference = property.getValue().get("capability-reference").asStringOrNull();
        String stability = property.getValue().get("stability").asStringOrNull();
        Collection<String> allowedValues = new HashSet<>();
        if (property.getValue().hasDefined("allowed")) {
            for (ModelNode allowed : property.getValue().get("allowed").asList()) {
                allowedValues.add(allowed.asString());
            }
        }
        return new Attribute(name, description, type, nilable, expressionsAllowed, defaultValue, min, max, accessType, storage, Deprecated.fromModel(property.getValue()), unit, restartRequired, capabilityReference, stability, allowedValues);
    }

    @Override
    public int compareTo(Attribute o) {
        return name.compareTo(o.name);
    }
}
