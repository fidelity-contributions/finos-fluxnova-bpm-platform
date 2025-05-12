package org.flowave.service;

import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MigratorServiceIntegrationTest {

    String tempDir = System.getProperty("java.io.tmpdir");

    private MigratorService migratorService;
    private String projectLocation;
    private Invoker mockInvoker;

    @BeforeEach
    void setUp() throws IOException {
        projectLocation = tempDir + File.separator + "test-project";
        Files.createDirectories(Path.of(projectLocation));
        projectLocation = tempDir + File.separator + "test-project" + File.separator;
        migratorService = new MigratorService(projectLocation);
    }

    @Test
    void testStartMethodReplacesOrgCamundaWithOrgFlowave() throws IOException, XmlPullParserException, MavenInvocationException {
        // Create a mock project structure
        createMockProjectStructure();

        // Run the migration
        migratorService.start();

        // Verify the results
        verifyMigrationResults();

    }

    private void createMockProjectStructure() throws IOException {
        // Create pom.xml
        String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>org.example</groupId>\n" +
                "    <artifactId>test-project</artifactId>\n" +
                "    <version>1.0-SNAPSHOT</version>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>org.camunda.bpm</groupId>\n" +
                "            <artifactId>camunda-engine</artifactId>\n" +
                "            <version>7.15.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>";
        Files.writeString(Path.of(projectLocation + "pom.xml"), pomContent);

        // Create src/main/java directory structure
        Path srcMainJava = Path.of(projectLocation + "src/main/java/org/workflow/example");
        Files.createDirectories(srcMainJava);

        // Create a Java file with Camunda imports
        String javaContent = "package org.workflow.example;\n\n" +
                "import org.camunda.bpm.engine.ProcessEngine;\n" +
                "import org.camunda.bpm.engine.RuntimeService;\n\n" +
                "public class CamundaService {\n" +
                "    private final ProcessEngine processEngine;\n\n" +
                "    public CamundaService(ProcessEngine processEngine) {\n" +
                "        this.processEngine = processEngine;\n" +
                "    }\n\n" +
                "    public void startProcess(String processKey) {\n" +
                "        RuntimeService runtimeService = processEngine.getRuntimeService();\n" +
                "        runtimeService.startProcessInstanceByKey(processKey);\n" +
                "    }\n" +
                "}";
        Files.writeString(srcMainJava.resolve("CamundaService.java"), javaContent);

        // Create a BPMN file
        Path resourcesDir = Path.of(projectLocation + "src/main/resources");
        Files.createDirectories(resourcesDir);
        String bpmnContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                "                  targetNamespace=\"http://camunda.org/examples\">\n" +
                "  <bpmn:process id=\"exampleProcess\" name=\"Example Process\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" camunda:initiator=\"starter\" />\n" +
                "    <bpmn:endEvent id=\"endEvent_1\" mycamunda:initiator=\"end\" />\n" +
                "    <bpmn:endEvent id=\"endEvent_1\" camundaprocess:initiator=\"end\" />\n" +
                "  </bpmn:process>\n" +
                "</bpmn:definitions>";
        Files.writeString(resourcesDir.resolve("process.bpmn"), bpmnContent);
    }

    private void verifyMigrationResults() throws IOException, MavenInvocationException {
        // Verify POM dependencies were updated
        String updatedPom = Files.readString(Path.of(projectLocation + "pom.xml"));
        assertTrue(updatedPom.contains("org.flowave.bpm"));
        assertFalse(updatedPom.contains("org.camunda.bpm"));

        // Verify Java package and imports were updated
        Path migratedJavaFile = Path.of(projectLocation + "src/main/java/org/workflow/example/CamundaService.java");

        String javaContent = Files.readString(migratedJavaFile);
        assertTrue(javaContent.contains("package org.workflow.example;"));
        assertTrue(javaContent.contains("import org.flowave.bpm.engine.ProcessEngine;"));
        assertTrue(javaContent.contains("import org.flowave.bpm.engine.RuntimeService;"));
        assertFalse(javaContent.contains("org.camunda"));

        // Verify BPMN file was converted back from XML
        Path bpmnFile = Path.of(projectLocation + "src/main/resources/process.bpmn");
        assertTrue(Files.exists(bpmnFile), "BPMN file should exist");

        String bpmnContent = Files.readString(bpmnFile);
        assertTrue(bpmnContent.contains("flowave:initiator=\"starter\""));
        assertFalse(bpmnContent.contains("camunda:initiator=\"starter\""));

        assertTrue(bpmnContent.contains("mycamunda:initiator=\"end\""));
        assertTrue(bpmnContent.contains("camundaprocess:initiator=\"end\""));

        // Verify rewrite.yml was deleted
        assertFalse(Files.exists(Path.of(projectLocation + "rewrite.yml")), "rewrite.yml should be deleted");

    }

}
