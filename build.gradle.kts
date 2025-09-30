plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

group = (project.findProperty("GROUP") as String?) ?: "io.github.itzamic"
version = (project.findProperty("VERSION_NAME") as String?) ?: "0.1.0-SNAPSHOT"

val micronautVersion = providers.gradleProperty("micronautVersion").getOrElse("4.5.0")

dependencies {
    // Micronaut core
    annotationProcessor("io.micronaut:micronaut-inject-java:$micronautVersion")
    implementation("io.micronaut:micronaut-context:$micronautVersion")
    implementation("io.micronaut:micronaut-runtime:$micronautVersion")
    implementation("io.micronaut:micronaut-http:$micronautVersion")
    implementation("io.micronaut:micronaut-http-server-netty:$micronautVersion")
    implementation("io.micronaut:micronaut-websocket:$micronautVersion")
    implementation("io.micronaut:micronaut-jackson-databind:$micronautVersion")
    // Jakarta annotations/inject APIs for Micronaut and lifecycle annotations
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // Logging API (do not force an implementation to avoid conflicts with host apps)
    api("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Eidolon",
            "Implementation-Version" to (project.findProperty("VERSION_NAME") as String? ?: "0.1.0-SNAPSHOT"),
            "Premain-Class" to "io.github.itzamic.eidolon.EidolonAgent",
            "Agent-Class" to "io.github.itzamic.eidolon.EidolonAgent",
            "Can-Redefine-Classes" to "false",
            "Can-Retransform-Classes" to "false"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = (project.findProperty("GROUP") as String?) ?: "io.github.itzamic"
            artifactId = (project.findProperty("POM_ARTIFACT_ID") as String?) ?: "eidolon"
            version = (project.findProperty("VERSION_NAME") as String?) ?: "0.1.0-SNAPSHOT"

            pom {
                name.set("Eidolon")
                description.set("JVM introspection plugin with embedded Micronaut HTTP/WebSocket server")
                url.set((project.findProperty("POM_URL") as String?) ?: "https://github.com/itzamic/eidolon")
                licenses {
                    license {
                        name.set((project.findProperty("POM_LICENCE_NAME") as String?) ?: "Apache-2.0")
                        url.set((project.findProperty("POM_LICENCE_URL") as String?) ?: "https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set((project.findProperty("POM_DEVELOPER_ID") as String?) ?: "itzamic")
                        name.set((project.findProperty("POM_DEVELOPER_NAME") as String?) ?: "itzamic")
                    }
                }
                scm {
                    url.set((project.findProperty("POM_SCM_URL") as String?) ?: "https://github.com/itzamic/eidolon")
                }
            }
        }
    }
    repositories {
        // Local test repository (change to your Nexus/Artifactory as needed)
        maven {
            name = "localBuildRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
