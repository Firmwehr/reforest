plugins {
    java
    application
    id("org.graalvm.buildtools.native") version "0.9.9"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.github.firmwehr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    val jbock = "5.12"
    implementation("io.github.jbock-java:jbock:$jbock")
    annotationProcessor("io.github.jbock-java:jbock-compiler:$jbock")

    implementation("fr.inria.gforge.spoon:spoon-core:10.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


application {
    mainClass.set("com.github.firmwehr.reforest.RandomProgramGenerator")
}

graalvmNative {
    binaries {
        named("main") {
            // Main options
            imageName.set("reforest") // The name of the native image, defaults to the project name
            mainClass.set("com.github.firmwehr.reforest.RandomProgramGenerator") // The main class to use, defaults to the application.mainClass
            debug.set(true) // Determines if debug info should be generated, defaults to false
            verbose.set(true) // Add verbose output, defaults to false
            sharedLibrary.set(false) // Determines if image is a shared library, defaults to false if `java-library` plugin isn't included
            buildArgs.add("--allow-incomplete-classpath")
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
