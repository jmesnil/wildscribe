package org.wildfly.wildscribe.wildflydoc;

import java.io.IOException;
import java.nio.file.Path;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

@QuarkusMain
@TopCommand
@CommandLine.Command(name = "wildflydoc", mixinStandardHelpOptions = true)
public class WildFlyDoc implements Runnable, QuarkusApplication {
    @Inject
    CommandLine.IFactory factory;

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).execute(args);
    }

    @CommandLine.Option(names = {"-f", "--feature-pack"}, required = true, description = "Feature pack GAV")
    String featurePackGav;

    @CommandLine.Option(names = {"-m", "--model"}, required = true, description = "Model reference file (in JSON)")
    Path modelFile;

    @CommandLine.Option(names = {"-o", "--output"}, required = true, description = "Output directory")
    Path outputDirectory;

    private final SiteGenerator siteGenerator;

    public WildFlyDoc(SiteGenerator siteGenerator) {
        this.siteGenerator = siteGenerator;
    }

    @Override
    public void run() {
        try {
            siteGenerator.generate(featurePackGav, modelFile, outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

