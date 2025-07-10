package org.finos.flowave;

import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.finos.flowave.service.MigratorService;

import java.io.*;

/**
 * Main entry point for the Flowave migration tool.
 * This class provides a command-line interface for migrating Camunda projects to Flowave.
 * It delegates the actual migration work to the {@link MigratorService} class.
 * 
 * <p>Usage: java -jar flowave-migrator.jar "/path/to/project"</p>
 */
public class Migrator {
    
    /**
     * The main method that initiates the migration process.
     * 
     * @param args Command line arguments. Expects exactly one argument:
     *             the path to the root directory of the project to be migrated.
     * @throws IOException If there is an error reading or writing files during migration
     * @throws XmlPullParserException If there is an error parsing XML files (e.g., pom.xml)
     * @throws MavenInvocationException If there is an error invoking Maven during the migration process
     */
    public static void main(String[] args) throws IOException, XmlPullParserException, MavenInvocationException {
        if(args.length != 1) {
            System.out.println("Please pass the root folder of the project to be migrated as a parameter in command line");
        } else {
            System.out.println(String.format("Migration from Camunda to Flowave started on project %s", args[0]));
            MigratorService migratorService = new MigratorService(args[0]);
            migratorService.start();
            System.out.println("Migrating from Camunda to Flowave ended");
        }
    }
}
