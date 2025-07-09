package org.finos.flowave.service;

import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.finos.flowave.Migrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigratorServiceTest {

    String tempDir = System.getProperty("java.io.tmpdir");

    private MigratorService migratorService;
    private String projectLocation;

    @Mock
    private MavenXpp3Reader mavenReader;

    @Mock
    private MavenXpp3Writer mavenWriter;

    @Mock
    private Invoker invoker;

    @Mock
    private InputStream mockInputStream;

    @BeforeEach
    void setUp() throws IOException {
        projectLocation = tempDir + File.separator;
        migratorService = new MigratorService(projectLocation);

        // Create a minimal pom.xml file in the temp directory
        Path pomPath = Path.of(projectLocation + "pom.xml");
        String minimalPom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0-SNAPSHOT</version>
                <build>
                    <plugins>
                    </plugins>
                </build>
            </project>""";
        Files.writeString(pomPath, minimalPom);
    }

    @Test
    void testReadPomToModel() throws Exception {
        // This test uses the real implementation to read a pom file
        Model model = migratorService.readPomToModel(projectLocation + "pom.xml");

        assertNotNull(model);
        assertEquals("org.example", model.getGroupId());
        assertEquals("test-project", model.getArtifactId());
        assertEquals("1.0-SNAPSHOT", model.getVersion());
    }

    @Test
    void testAddPlugin() throws Exception {
        // Read the model from the test pom file
        Model model = migratorService.readPomToModel(projectLocation + "pom.xml");

        // Use reflection to access the private method
        java.lang.reflect.Method addPluginMethod = MigratorService.class.getDeclaredMethod("addPlugin", Model.class);
        addPluginMethod.setAccessible(true);
        addPluginMethod.invoke(migratorService, model);

        // Verify the plugin was added correctly
        List<Plugin> plugins = model.getBuild().getPlugins();
        assertEquals(1, plugins.size());

        Plugin plugin = plugins.get(0);
        assertEquals("org.openrewrite.maven", plugin.getGroupId());
        assertEquals("rewrite-maven-plugin", plugin.getArtifactId());
        assertEquals("6.3.0", plugin.getVersion());

        // Verify configuration
        Xpp3Dom config = (Xpp3Dom) plugin.getConfiguration();
        assertNotNull(config);

        Xpp3Dom activeRecipes = config.getChild("activeRecipes");
        assertNotNull(activeRecipes);

        Xpp3Dom[] recipeNodes = activeRecipes.getChildren("recipe");
        assertEquals(1, recipeNodes.length);
        assertEquals("camundaToFlowave", recipeNodes[0].getValue());
    }

    @Test
    void testAddDependencies() throws Exception {
        // Read the model from the test pom file
        Model model = migratorService.readPomToModel(projectLocation + "pom.xml");

        // Use reflection to access the private method
        java.lang.reflect.Method addDependenciesMethod = MigratorService.class.getDeclaredMethod("addDependencies", Model.class);
        addDependenciesMethod.setAccessible(true);
        addDependenciesMethod.invoke(migratorService, model);

        // Verify dependencies were added correctly
        DependencyManagement depMgmt = model.getDependencyManagement();
        assertNotNull(depMgmt);

        List<Dependency> dependencies = depMgmt.getDependencies();
        assertEquals(2, dependencies.size());

        // Check first dependency
        Dependency dep1 = dependencies.get(0);
        assertEquals("org.openrewrite", dep1.getGroupId());
        assertEquals("rewrite-core", dep1.getArtifactId());
        assertEquals("7.0.0", dep1.getVersion());

        // Check second dependency
        Dependency dep2 = dependencies.get(1);
        assertEquals("org.openrewrite", dep2.getGroupId());
        assertEquals("rewrite-java", dep2.getArtifactId());
        assertEquals("7.0.0", dep2.getVersion());
    }

    @Test
    void testCopyRewriteYml() throws Exception {
        try (MockedStatic<Migrator> migratorMockedStatic = mockStatic(Migrator.class)) {
            // Setup ClassLoader mock
            ClassLoader mockClassLoader = mock(ClassLoader.class);
            //lenient(mockClassLoader.getResourceAsStream("rewrite.yml")).thenReturn(new ByteArrayInputStream("test content".getBytes()));

            // Use reflection to access the private method
            java.lang.reflect.Method copyRewriteYmlMethod = MigratorService.class.getDeclaredMethod("copyRewriteYml");
            copyRewriteYmlMethod.setAccessible(true);
            copyRewriteYmlMethod.invoke(migratorService);

            // Verify the file was created
            Path rewriteYmlPath = Path.of(projectLocation + "rewrite.yml");
            assertTrue(Files.exists(rewriteYmlPath));
        }
    }

    @Test
    void removeDependencies_shouldRemoveOpenrewriteDependencies() {
        // Arrange
        Model model = new Model();
        DependencyManagement dependencyManagement = new DependencyManagement();

        Dependency keepDependency = new Dependency();
        keepDependency.setGroupId("org.example");
        keepDependency.setArtifactId("example-lib");

        Dependency removeDependency1 = new Dependency();
        removeDependency1.setGroupId("org.openrewrite");
        removeDependency1.setArtifactId("rewrite-core");

        Dependency removeDependency2 = new Dependency();
        removeDependency2.setGroupId("org.openrewrite");
        removeDependency2.setArtifactId("rewrite-java");

        dependencyManagement.addDependency(keepDependency);
        dependencyManagement.addDependency(removeDependency1);
        dependencyManagement.addDependency(removeDependency2);

        model.setDependencyManagement(dependencyManagement);

        // Act
        migratorService.removeDependencies(model);

        // Assert
        List<Dependency> remainingDependencies = model.getDependencyManagement().getDependencies();
        assertEquals(1, remainingDependencies.size());
        assertEquals("org.example", remainingDependencies.get(0).getGroupId());
        assertEquals("example-lib", remainingDependencies.get(0).getArtifactId());
    }

    @Test
    void removeDependencies_shouldHandleNullDependencyManagement() {
        // Arrange
        Model model = new Model();
        model.setDependencyManagement(null);

        // Act & Assert (should not throw exception)
        assertDoesNotThrow(() -> migratorService.removeDependencies(model));
    }

    @Test
    void removePlugin_shouldRemoveRewriteMavenPlugin() {
        // Arrange
        Model model = new Model();
        Build build = new Build();

        Plugin keepPlugin = new Plugin();
        keepPlugin.setGroupId("org.example");
        keepPlugin.setArtifactId("example-plugin");

        Plugin removePlugin = new Plugin();
        removePlugin.setGroupId("org.openrewrite.maven");
        removePlugin.setArtifactId("rewrite-maven-plugin");

        build.addPlugin(keepPlugin);
        build.addPlugin(removePlugin);

        model.setBuild(build);

        // Act
        migratorService.removePlugin(model);

        // Assert
        List<Plugin> remainingPlugins = model.getBuild().getPlugins();
        assertEquals(1, remainingPlugins.size());
        assertEquals("org.example", remainingPlugins.get(0).getGroupId());
        assertEquals("example-plugin", remainingPlugins.get(0).getArtifactId());
    }

    @Test
    void removePlugin_shouldHandleNullBuild() {
        // Arrange
        Model model = new Model();
        model.setBuild(null);

        // Act & Assert (should not throw exception)
        assertDoesNotThrow(() -> migratorService.removePlugin(model));
    }

    @Test
    void deleteRewriteYml_shouldDeleteFileIfExists() throws IOException {
        // Arrange
        Path rewriteYmlPath = Path.of(projectLocation + "rewrite.yml");
        Files.writeString(rewriteYmlPath, "dummy content");

        // Act
        migratorService.deleteRewriteYml();

        // Assert
        assertFalse(Files.exists(rewriteYmlPath));
    }

    @Test
    void deleteRewriteYml_shouldHandleNonExistentFile() {
        // Arrange - ensure file doesn't exist
        Path rewriteYmlPath = Path.of(projectLocation + "rewrite.yml");
        assertFalse(Files.exists(rewriteYmlPath));

        // Act & Assert (should not throw exception)
        assertDoesNotThrow(() -> migratorService.deleteRewriteYml());
    }

    @Test
    void convertBpmnToXml_ShouldRenameFiles() throws IOException {
        Path bpmnFile = Path.of(projectLocation + "process.bpmn");
        Files.deleteIfExists(bpmnFile);
        // Create test BPMN files
        Files.createFile(bpmnFile);

        // Execute conversion
        migratorService.convertBpmnToXml(new File(tempDir));

        Path processXml = Path.of(projectLocation + "process__bpmn__.xml");
        assertTrue(Files.exists(processXml), "Converted XML file should exist");

        Files.deleteIfExists(processXml);

    }

    @Test
    void convertXmlToBpmn_ShouldRenameFiles() throws IOException {
        // Create test converted XML files
        Path xmlFile = Path.of(projectLocation + "process__bpmn__.xml");
        Files.deleteIfExists(xmlFile);
        Files.createFile(xmlFile);

        // Execute conversion
        migratorService.convertXmlToBpmn(new File(tempDir));

        Path processBpmn = Path.of(projectLocation + "process.bpmn");
        assertTrue(Files.exists(processBpmn), "BPMN file should exist");
        Files.deleteIfExists(processBpmn);
    }

}