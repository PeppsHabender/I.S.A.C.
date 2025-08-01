import java.time.Instant
import java.util.Properties

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.allopen") version "2.0.21"
    id("io.quarkus")
    id("com.diffplug.spotless") version "7.0.2"
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    compileOnly("org.jetbrains.lets-plot:lets-plot-batik:4.6.1")
    compileOnly("org.jetbrains.lets-plot:lets-plot-kotlin:4.2.0")

    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-mongodb-client")
    implementation("io.quarkus:quarkus-mongodb-panache-kotlin")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-smallrye-fault-tolerance")

    implementation("org.jetbrains.kotlinx:kandy-lets-plot:0.8.0") {
        exclude(group = "xml-apis", module = "xml-apis")
    }
    implementation("com.discord4j:discord4j-core:3.3.0-RC1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

group = "org.inquest"
version = "1.0.5-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

spotless {
    kotlin {
        ktlint().editorConfigOverride(mapOf(
            "ktlint_function_signature_body_expression_wrapping" to "default",
            "ktlint_code_style" to "intellij_idea",
            "max_line_length" to 140
        ))
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        javaParameters = true
    }
}

tasks.quarkusDev {
    workingDirectory = rootProject.projectDir
}

val cpyInfo = tasks.register("copyBuildInfo") {
    val timestamp = Instant.now().toEpochMilli()

    val path = project.projectDir.resolve("src/main/resources/buildInfo.properties")
    if(!path.exists()) path.createNewFile()

    Properties().apply {
        put("buildTime", timestamp.toString())
        put("version", version.toString())
    }.store(path.outputStream(), null)
}

tasks.findByName("build")?.dependsOn(cpyInfo)