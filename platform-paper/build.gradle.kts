import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
    `java-library`
    id("com.gradleup.shadow")
}

val projectVersion = version.toString()
val minimumPaperApi =
    "io.papermc.paper:paper-api:${libs.versions.paper.minimum.get()}@jar"
val minimumGeyserApi =
    "org.geysermc.geyser:api:${libs.versions.geyser.minimum.get()}@jar"
val currentPaperCompileClasspath = configurations.create("currentPaperCompileClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val javaToolchains = extensions.getByType<JavaToolchainService>()

dependencies {
    api(project(":core"))
    compileOnly(minimumPaperApi)
    compileOnly(minimumGeyserApi)
    compileOnly(libs.floodgate.api)
    compileOnly(libs.adventure.api)
    currentPaperCompileClasspath(project(":core"))
    currentPaperCompileClasspath(libs.paper.api)
    currentPaperCompileClasspath(libs.geyser.api)
    currentPaperCompileClasspath(libs.floodgate.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(minimumPaperApi)
    testImplementation(libs.adventure.api)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val compileCurrentPaperJava = tasks.register<JavaCompile>("compileCurrentPaperJava") {
    source(sourceSets.main.get().java)
    classpath = currentPaperCompileClasspath
    destinationDirectory.set(layout.buildDirectory.dir("classes/java/currentPaper"))
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    options.release.set(17)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to projectVersion)
    }
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveFileName.set("hurricane-paper-$projectVersion.jar")
    archiveClassifier.set("")
    from(rootProject.file("LICENSE"))
}

tasks.build {
    dependsOn(compileCurrentPaperJava)
    dependsOn(tasks.shadowJar)
}
