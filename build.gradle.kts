plugins {
    java
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

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
