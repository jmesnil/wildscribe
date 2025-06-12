package org.jboss.wildscribe.site;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(final String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("USAGE: java [-Durl=http://wildscribe.github.io] -jar site-generator.jar model-directory output-directory");
                System.exit(1);
            }
            Path modelFile = Paths.get(args[0]);
            Path outputDirectory = Paths.get(args[1]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
