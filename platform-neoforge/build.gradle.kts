plugins {
    id("com.gradleup.shadow")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    mods {
        create("hurricane-neoforge") {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":platform-modded-common").extensions
                .getByType<SourceSetContainer>()
                .named("main")
                .get())
            sourceSet(project(":core").extensions
                .getByType<SourceSetContainer>()
                .named("main")
                .get())
        }
    }
}

val common = configurations.create("common")
val developmentNeoForge: Configuration = configurations.getByName("developmentNeoForge")
val projectVersion = version.toString()

configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    developmentNeoForge.extendsFrom(common)
}

dependencies {
    neoForge(libs.neoforge)
    runtimeOnly(project(":core"))

    common(project(":platform-modded-common")) {
        isTransitive = false
    }
    shadow(project(path = ":platform-modded-common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
    shadow(project(":core"))
}

tasks.processResources {
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to projectVersion)
    }
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("shadow"))
    archiveFileName.set("hurricane-neoforge-$projectVersion.jar")
    archiveClassifier.set("")
    from(rootProject.file("LICENSE"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
