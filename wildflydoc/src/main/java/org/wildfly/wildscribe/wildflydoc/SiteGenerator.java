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
import java.util.ArrayList;

@Dependent
public class SiteGenerator {

    @Inject
    Template resource;

    @Inject
    Template index;

    void generate(String featurePackGAV, Path modelFile, Path outputDirectory) throws IOException {
        System.out.println("🔎 Generating Feature Pack Documentation");
        System.out.println("  - Feature Pack: " + featurePackGAV);
        System.out.println("  - Model: " + modelFile);

        Files.createDirectories(outputDirectory);

        try (InputStream in = Files.newInputStream(modelFile)) {
            ModelNode rootDescription = ModelNode.fromJSONStream(in);

            generateIndex(outputDirectory, featurePackGAV, rootDescription);
            //System.out.println(rootDescription.toJSONString(false).substring(0, 500));
            //System.out.println(rootDescription.keys());

            if (rootDescription.has("children", "subsystem", "model-description")) {
                for (Property prop : rootDescription.get("children", "subsystem", "model-description").asPropertyList()) {
                    //System.out.println("subsystem = " + prop.getName());
                    String subsystemName = prop.getName();
                    String description = prop.getValue().get("description").asStringOrNull();

                    String content = resource.data("name", subsystemName,
                                    "description", description,
                                    "address", "/subsystem=" + subsystemName)
                            .render();
                    writeToFile(content, outputDirectory, "subsystem", subsystemName, "index.html");
                }
            }
        }

        System.out.println("✏️ Site generated at " + outputDirectory);
    }

    private static void writeToFile(String content, Path baseDir, String... subPaths) throws IOException {
        Path resolvedPath = baseDir;
        for (String subPath : subPaths) {
            resolvedPath = resolvedPath.resolve(subPath);
        }
        Path parentDir = resolvedPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.writeString(resolvedPath, content, StandardCharsets.UTF_8);
    }

    private void generateIndex(Path outputDirectory, String featurePackGAV, ModelNode rootDescription) throws IOException {

        if (rootDescription.has("children", "subsystem", "model-description")) {

        }
        final String currentUrl = buildCurrentUrl();

        String content = index.data("feature-pack", featurePackGAV)
                .data("currentUrl", currentUrl)
                .data("breadcrumbs", Breadcrumb.build(new PathElement[] {}))
                .data("children", new ArrayList<>())
                .render();
        writeToFile(content, outputDirectory,  "index.html");
    }

    private static String buildCurrentUrl(final PathElement... path) {
        StringBuilder sb = new StringBuilder();
        for (PathElement i : path) {
            if (!sb.toString().isEmpty()) {
                sb.append('/');
            }
            sb.append(i.getKey());
            if (!i.isWildcard()) {
                sb.append('/');
                sb.append(i.getValue());
            }
        }
        return sb.toString();
    }

    private PathElement[] addToPath(PathElement[] path, final String key, final String value) {
        PathElement[] newPath = new PathElement[path.length + 1];
        System.arraycopy(path, 0, newPath, 0, path.length);
        newPath[path.length] = new PathElement(key, value);
        return newPath;
    }
}
