package org.wildfly.wildscribe.wildflydoc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.qute.Template;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jboss.dmr.ModelNode;

import java.nio.charset.StandardCharsets;

@Dependent
public class SiteGenerator {

    @Inject
    Template resource;

    void generate(Path modelFile, Path outputDirectory) throws IOException {
        System.out.println("🔎 Generating documentation from " + modelFile);

        Files.createDirectories(outputDirectory);

        try (InputStream in = Files.newInputStream(modelFile)) {
            ModelNode description = ModelNode.fromJSONStream(in);
        }

        String content = resource.data("name", "foo").render();
        Files.writeString(outputDirectory.resolve("foo.html"), content, StandardCharsets.UTF_8);

        System.out.println("✏️ Site generated at " + outputDirectory);
    }
}
