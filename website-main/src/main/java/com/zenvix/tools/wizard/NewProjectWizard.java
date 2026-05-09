package com.zenvix.tools.wizard;

import com.zenvix.core.services.MavenManager;
import com.zenvix.tools.projects.ProjectManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * NewProjectWizard orchestrates project generation across multiple frameworks
 * mapping Spring Boot via Initializr, Maven Archetypes, Gradle standard layouts, and Jakarta EE templates.
 */
public class NewProjectWizard {

    public enum TemplateType {
        SPRING_BOOT, MAVEN_ARCHETYPE, GRADLE_TEMPLATE, JAKARTA_EE
    }

    private final MavenManager mavenManager;
    private final ProjectManager projectManager;

    public NewProjectWizard(MavenManager mavenManager, ProjectManager projectManager) {
        this.mavenManager = mavenManager;
        this.projectManager = projectManager;
    }

    public static class ProjectSpecs {
        public TemplateType type;
        public String groupId = "com.example";
        public String artifactId = "demo";
        public String version = "1.0.0";
        public String javaVersion = "17";
        public List<String> dependencies = new ArrayList<>();
        public String bootVersion = "3.2.3";
        public String buildTool = "maven-project"; 
        public Path destination;
    }

    // --- Generator Dispatcher ---

    public void generateProject(ProjectSpecs specs, Consumer<String> progressCallback) throws Exception {
        if (specs.destination == null) throw new IllegalArgumentException("Destination is required.");
        
        if (!Files.exists(specs.destination)) {
            Files.createDirectories(specs.destination);
        }

        switch (specs.type) {
            case SPRING_BOOT:
                progressCallback.accept("Generating Spring Boot project via start.spring.io...");
                generateSpringBoot(specs);
                break;
            case MAVEN_ARCHETYPE:
                progressCallback.accept("Executing Maven Archetype generator...");
                generateMavenArchetype(specs, progressCallback);
                break;
            case GRADLE_TEMPLATE:
                progressCallback.accept("Generating Gradle Template structure...");
                generateGradleTemplate(specs);
                break;
            case JAKARTA_EE:
                progressCallback.accept("Scaffolding Jakarta EE application...");
                generateJakartaEE(specs);
                break;
        }

        progressCallback.accept("Importing into Project Manager...");
        projectManager.importProject(specs.destination.resolve(specs.artifactId).toAbsolutePath().toString());
        progressCallback.accept("Project creation complete.");
    }

    // --- Spring Initializr ---

    protected void generateSpringBoot(ProjectSpecs specs) throws Exception {
        String baseUrl = "https://start.spring.io/starter.zip";
        
        StringBuilder postData = new StringBuilder();
        postData.append("type=").append(URLEncoder.encode(specs.buildTool, "UTF-8"));
        postData.append("&language=java");
        postData.append("&bootVersion=").append(URLEncoder.encode(specs.bootVersion, "UTF-8"));
        postData.append("&groupId=").append(URLEncoder.encode(specs.groupId, "UTF-8"));
        postData.append("&artifactId=").append(URLEncoder.encode(specs.artifactId, "UTF-8"));
        postData.append("&name=").append(URLEncoder.encode(specs.artifactId, "UTF-8"));
        postData.append("&version=").append(URLEncoder.encode(specs.version, "UTF-8"));
        postData.append("&javaVersion=").append(URLEncoder.encode(specs.javaVersion, "UTF-8"));
        
        if (!specs.dependencies.isEmpty()) {
            postData.append("&dependencies=").append(URLEncoder.encode(String.join(",", specs.dependencies), "UTF-8"));
        }

        byte[] postBytes = postData.toString().getBytes("UTF-8");
        Path zipPath = specs.destination.resolve("starter.zip");

        URL url = new URL(baseUrl);
        downloadSpringZip(url, postBytes, zipPath);

        Path targetDir = specs.destination.resolve(specs.artifactId);
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(targetDir.toFile(), entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    Files.copy(zis, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Files.deleteIfExists(zipPath);
    }

    protected void downloadSpringZip(URL url, byte[] postBytes, Path zipPath) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postBytes.length));
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postBytes);
        }

        if (conn.getResponseCode() != 200) {
            throw new Exception("Spring Initializr API failed with code: " + conn.getResponseCode());
        }

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // --- Maven Archetype ---

    protected void generateMavenArchetype(ProjectSpecs specs, Consumer<String> progressCallback) throws Exception {
        String goals = String.format("archetype:generate -DgroupId=%s -DartifactId=%s -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false", 
                                     specs.groupId, specs.artifactId);
        
        mavenManager.runBuild(specs.destination.toString(), goals, progressCallback, null);
    }

    // --- Gradle Template ---

    protected void generateGradleTemplate(ProjectSpecs specs) throws Exception {
        Path targetDir = specs.destination.resolve(specs.artifactId);
        Files.createDirectories(targetDir);

        String buildGradle = "plugins {\n" +
                             "    id 'java'\n" +
                             "}\n\n" +
                             "group = '" + specs.groupId + "'\n" +
                             "version = '" + specs.version + "'\n" +
                             "sourceCompatibility = '" + specs.javaVersion + "'\n\n" +
                             "repositories {\n" +
                             "    mavenCentral()\n" +
                             "}\n\n" +
                             "dependencies {\n" +
                             "    testImplementation platform('org.junit:junit-bom:5.9.1')\n" +
                             "    testImplementation 'org.junit.jupiter:junit-jupiter'\n" +
                             "}\n\n" +
                             "test {\n" +
                             "    useJUnitPlatform()\n" +
                             "}\n";

        Files.write(targetDir.resolve("build.gradle"), buildGradle.getBytes());
        Files.write(targetDir.resolve("settings.gradle"), ("rootProject.name = '" + specs.artifactId + "'\n").getBytes());

        String packagePath = specs.groupId.replace(".", "/");
        Path mainJava = targetDir.resolve("src/main/java").resolve(packagePath);
        Files.createDirectories(mainJava);

        String mainClass = "package " + specs.groupId + ";\n\n" +
                           "public class Main {\n" +
                           "    public static void main(String[] args) {\n" +
                           "        System.out.println(\"Hello World from Gradle!\");\n" +
                           "    }\n" +
                           "}\n";
        
        Files.write(mainJava.resolve("Main.java"), mainClass.getBytes());
    }

    // --- Jakarta EE Template ---

    protected void generateJakartaEE(ProjectSpecs specs) throws Exception {
        Path targetDir = specs.destination.resolve(specs.artifactId);
        Files.createDirectories(targetDir);

        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                     "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                     "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                     "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                     "    <modelVersion>4.0.0</modelVersion>\n" +
                     "    <groupId>" + specs.groupId + "</groupId>\n" +
                     "    <artifactId>" + specs.artifactId + "</artifactId>\n" +
                     "    <version>" + specs.version + "</version>\n" +
                     "    <packaging>war</packaging>\n" +
                     "    <properties>\n" +
                     "        <maven.compiler.source>" + specs.javaVersion + "</maven.compiler.source>\n" +
                     "        <maven.compiler.target>" + specs.javaVersion + "</maven.compiler.target>\n" +
                     "    </properties>\n" +
                     "    <dependencies>\n" +
                     "        <dependency>\n" +
                     "            <groupId>jakarta.platform</groupId>\n" +
                     "            <artifactId>jakarta.jakartaee-api</artifactId>\n" +
                     "            <version>10.0.0</version>\n" +
                     "            <scope>provided</scope>\n" +
                     "        </dependency>\n" +
                     "    </dependencies>\n" +
                     "    <build>\n" +
                     "        <finalName>${project.artifactId}</finalName>\n" +
                     "    </build>\n" +
                     "</project>\n";

        Files.write(targetDir.resolve("pom.xml"), pom.getBytes());

        Path webInf = targetDir.resolve("src/main/webapp/WEB-INF");
        Files.createDirectories(webInf);

        String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd\"\n" +
                        "         version=\"6.0\">\n" +
                        "</web-app>\n";

        Files.write(webInf.resolve("web.xml"), webXml.getBytes());

        String packagePath = specs.groupId.replace(".", "/");
        Path mainJava = targetDir.resolve("src/main/java").resolve(packagePath);
        Files.createDirectories(mainJava);
    }
}
