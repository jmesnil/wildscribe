package org.wildfly.wildscribe.wildflydoc;

import static org.wildfly.wildscribe.wildflydoc.PathAddress.EMPTY_ADDRESS;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jboss.dmr.ModelNode;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Dependent
public class SiteGenerator {

    @Inject
    @Location("resource.html")
    Template resourceTemplate;

    @Inject
    Template sidebar;

    @Inject
    Template index;

    void generate(String featurePackGAV, Path modelFile, Path outputDirectory) throws IOException {
        System.out.println("🔎 Generating Feature Pack Documentation");
        System.out.println("  - Feature Pack: " + featurePackGAV);
        System.out.println("  - Model: " + modelFile);

        Files.createDirectories(outputDirectory);

        try (InputStream in = Files.newInputStream(modelFile)) {
            ModelNode rootDescription = ModelNode.fromJSONStream(in);

            //generateSidebar(outputDirectory, rootDescription);
            generate(outputDirectory, featurePackGAV, rootDescription);
            //generateIndex(outputDirectory, rootDescription);
        }

        System.out.println("✏️ Site generated at " + outputDirectory);
    }

    private static void writeToFile(String content, Path file) throws IOException {
        Path parentDir = file.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private void generateSidebar(Path outputDirectory, ModelNode rootDescription) throws IOException {
        final String currentUrl = buildCurrentUrl();
        ResourceNode root = ResourceNode.fromModelNode("", currentUrl, "home", EMPTY_ADDRESS, rootDescription);
        String content = sidebar.data("resource", root)
                .render();
        writeToFile(content, outputDirectory.resolve("sidebar.html"));
    }

    private void generateIndex(Path outputDirectory, ModelNode rootDescription) throws IOException {
        final String currentUrl = buildCurrentUrl();
        ResourceNode root = ResourceNode.fromModelNode("", currentUrl, "home", EMPTY_ADDRESS, rootDescription);
        final String relativePathToContextRoot = createRelativePathToContextRoot(currentUrl);
        String content = index.data("resource", root)
                .data("currentUrl", currentUrl)
                .data("resource", root)
                .data("breadcrumbs", Breadcrumb.build())
                .render();
        writeToFile(content, outputDirectory.resolve("index.html"));
    }
    private void generate(Path outputDirectory, String featurePackGAV, ModelNode rootDescription, PathElement... path) throws IOException {
        final Resource resource = Resource.fromModelNode(PathAddress.pathAddress(), rootDescription, Collections.emptyMap());
        final String currentUrl = buildCurrentUrl(path);
        final String relativePathToContextRoot = createRelativePathToContextRoot(currentUrl);

        String content = resourceTemplate.data("feature-pack", featurePackGAV)
                .data("currentUrl", currentUrl)
                .data("resource", resource)
                .data("breadcrumbs", Breadcrumb.build(path))
                .data("relativePathToContextRoot", relativePathToContextRoot)
                .render();
        Path dir = outputDirectory.resolve(currentUrl).normalize();
        writeToFile(content, dir.resolve("index.html"));

        for (Child child : resource.children()) {
            if (child.children().isEmpty()) {
                PathElement[] newPath = addToPath(path, child.name(), "*");
                ModelNode childModel = rootDescription.get("children").get(child.name());
                if (childModel.hasDefined("model-description")) {
                    ModelNode newModel = childModel.get("model-description").get("*");
                    if (!newModel.hasDefined("operations")) {
                        newModel.get("operations");
                    }
                    generate(outputDirectory, "", newModel, newPath);
                }
            } else {
                for (Child registration : child.children()) {
                    PathElement[] newPath = addToPath(path, child.name(), registration.name());

                    ModelNode childModel = rootDescription.get("children").get(child.name());
                    if (childModel.hasDefined("model-description") && childModel.get("model-description").hasDefined(registration.name())) {
                        ModelNode newModel = childModel.get("model-description").get(registration.name());
                        generate(outputDirectory, "", newModel, newPath);
                    }
                }
            }
        }
    }

    static String buildCurrentUrl(final PathElement... path) {
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

    static String buildCurrentUrl(final PathAddress address) {
        StringBuilder sb = new StringBuilder();
        for (PathElement i : address) {
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

    static String createRelativePathToContextRoot(String relativeUrl) {
        StringBuilder sb = new StringBuilder();
        int length = relativeUrl.isEmpty() ? 0 : relativeUrl.split("/").length;
        for (int i = 0; i < length; i++) {
            sb.append("../");
        }
        return sb.toString();
    }

    private static PathElement[] addToPath(PathElement[] path, final String key, final String value) {
        PathElement[] newPath = new PathElement[path.length + 1];
        System.arraycopy(path, 0, newPath, 0, path.length);
        newPath[path.length] = new PathElement(key, value);
        return newPath;
    }
}
