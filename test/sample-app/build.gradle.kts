plugins {
    application
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    // Pull the locally published Eidolon artifact
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.itzamic:eidolon:0.1.0-SNAPSHOT")
}

application {
    mainClass.set("com.example.App")
}

val eidolonVersion = "0.1.0-SNAPSHOT"

// Run with programmatic start (Eidolon.startDefault())
tasks.register<JavaExec>("runProgrammatic") {
    group = "application"
    description = "Run sample app with programmatic Eidolon.startDefault()"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.App")
}

// Run attaching Eidolon as a -javaagent and disable programmatic start in the app
tasks.register<JavaExec>("runAgent") {
    group = "application"
    description = "Run sample app with Eidolon attached as -javaagent"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.App")
    jvmArgs(
        "-javaagent:${System.getProperty("user.home")}/.m2/repository/io/github/itzamic/eidolon/$eidolonVersion/eidolon-$eidolonVersion.jar",
        "-Deidolon.disableProgrammaticStart=true"
    )
}
