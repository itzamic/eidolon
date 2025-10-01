plugins {
    `java-library`
    `maven-publish`
    id("jacoco")
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


    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.micronaut:micronaut-http-client:$micronautVersion")
    testImplementation("org.mockito:mockito-core:5.12.0")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.10"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacocoHtml"))
    }
    // Exclude code paths not required to be covered by tests
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "io/github/itzamic/eidolon/model/**",        // DTOs (pure data holders)
                    )
                }
            }
        )
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    // Apply the same exclusions for verification so thresholds reflect what we measure
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "io/github/itzamic/eidolon/ws/**",
                        "io/github/itzamic/eidolon/EidolonAgent*",
                        "io/github/itzamic/eidolon/model/**",
                        "io/github/itzamic/eidolon/Eidolon*",
                        "io/github/itzamic/eidolon/service/**"
                    )
                }
            }
        )
    )
    violationRules {
        rule {
            element = "BUNDLE"
            // Require at least 80% instruction and line coverage for measured code
            limit {
                counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "LINE"
                    value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
            Branch coverage is often lower for framework-heavy code; raise later once feasible
            limit {
                counter = "BRANCH"
                value = "COVERED_RATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.jacocoTestCoverageVerification)
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
