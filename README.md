Wildscribe Documentation Generator
==================================

This project is a documentation siteGenerator for Wildfly/JBoss EAP. Basically takes the self describing management model
and turns it into HTML. It consists of two parts, the model dumper and the site siteGenerator.

See models/README.md for details on dumping models.

To generate the site:

Add any new model versions to models/standalone/versions.txt

Then run (for example):

$ java -Durl=https://wildscribe.github.io -jar site-siteGenerator/target/site-siteGenerator.jar models/standalone/ ../wildscribe.github.io/
