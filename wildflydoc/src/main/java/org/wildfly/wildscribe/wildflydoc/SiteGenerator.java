package org.wildfly.wildscribe.wildflydoc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jboss.dmr.ModelNode;

@Dependent
public class SiteGenerator {

    @Inject
    @Location("resource.html")
    Template resourceTemplate;

    @Inject
    Template index;

    void generate(String featurePackGAV, Path modelFile, Path outputDirectory, String projectSourceLocation) throws IOException {
        System.out.println("🔎 Generating Feature Pack Documentation");
        System.out.println("  - Feature Pack: " + featurePackGAV);
        System.out.println("  - Model: " + modelFile);

        Files.createDirectories(outputDirectory);

        GAV gav = GAV.parse(featurePackGAV);

        Path docPath = outputDirectory.resolve("doc");
        Path referencePath = docPath.resolve("reference");
        Files.createDirectories(referencePath);

        try (InputStream in = Files.newInputStream(modelFile)) {
            ModelNode rootDescription = ModelNode.fromJSONStream(in);

            generateIndex(docPath, gav, projectSourceLocation);
            generateResource(referencePath, rootDescription);
        }

        System.out.println("✏️ Site generated at " + docPath);

        zipDirectory(outputDirectory, gav , docPath);
    }

    private void generateIndex(Path docPath, GAV gav, String projectSourceLocation) throws IOException {
        String content = index
                .data("gav", gav)
                .data("projectSourceLocation", projectSourceLocation).render();
        Files.writeString(docPath.resolve("index.html"), content, StandardCharsets.UTF_8);
    }

    private static void writeToFile(String content, Path file) throws IOException {
        Path parentDir = file.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    public static void zipDirectory(Path outputDirectory, GAV gav, Path sourceDirPath) throws IOException {

        Path zipPath = outputDirectory.resolve(String.format("%s-%s-doc.zip",
                gav.artifactId(), gav.version()));
        try (
                ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipPath));
                Stream<Path> stream = Files.walk(sourceDirPath)) {
            Path basePath = sourceDirPath.getParent(); // Ensures the root folder is included
            stream
                    .forEach(path -> {
                        try {
                            String zipEntryName = basePath.relativize(path).toString().replace("\\", "/");
                            if (Files.isDirectory(path)) {
                                if (!zipEntryName.endsWith("/")) {
                                    zipEntryName += "/";
                                }
                                zs.putNextEntry(new ZipEntry(zipEntryName));
                                zs.closeEntry();
                            } else {
                                zs.putNextEntry(new ZipEntry(zipEntryName));
                                Files.copy(path, zs);
                                zs.closeEntry();
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
        System.out.println("📁 Archive generated at " + zipPath);
    }

    private void generateResource(Path outputDirectory, ModelNode
            rootDescription, PathElement... path) throws IOException {
        PathAddress address = PathAddress.pathAddress(path);
        final Resource resource = Resource.fromModelNode(address, rootDescription, Collections.emptyMap());
        final String currentUrl = buildCurrentUrl(path);
        final String relativePathToContextRoot = createRelativePathToContextRoot(currentUrl);

        String content = resourceTemplate
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
                    if (newModel.hasDefined("operations")) {
                        newModel.get("operations");
                    }
                    generateResource(outputDirectory, newModel, newPath);
                }
            } else {
                for (Child registration : child.children()) {
                    PathElement[] newPath = addToPath(path, child.name(), registration.name());

                    ModelNode childModel = rootDescription.get("children").get(child.name());
                    if (childModel.hasDefined("model-description") && childModel.get("model-description").hasDefined(registration.name())) {
                        ModelNode newModel = childModel.get("model-description").get(registration.name());
                        generateResource(outputDirectory, newModel, newPath);
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
