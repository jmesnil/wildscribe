package org.wildfly.wildscribe.wildflydoc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.qute.Template;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.nio.charset.StandardCharsets;

@Dependent
public class SiteGenerator {

    @Inject
    Template resource;

    void generate(Path modelFile, Path outputDirectory) throws IOException {
        System.out.println("🔎 Generating documentation from " + modelFile);

        Files.createDirectories(outputDirectory);

        try (InputStream in = Files.newInputStream(modelFile)) {
            ModelNode rootDescription = ModelNode.fromJSONStream(in);

            System.out.println(rootDescription.toJSONString(false).substring(0, 500));
            System.out.println(rootDescription.keys());

            if (rootDescription.has("children", "subsystem", "model-description")) {
                for (Property prop : rootDescription.get("children", "subsystem", "model-description").asPropertyList()) {
                    System.out.println("subsystem = " + prop.getName());
                    String subsystemName = prop.getName();
                    String description = prop.getValue().get("description").asStringOrNull();

                    String content = resource.data("name", subsystemName,
                                    "description", description,
                                    "address", "/subsystem=" + subsystemName)
                            .render();
                    Files.writeString(outputDirectory.resolve(subsystemName + ".html"), content, StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println("✏️ Site generated at " + outputDirectory);
    }
}
