plugins {
    id("com.gradleup.shadow")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    mods {
        create("hurricane-fabric") {
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
val developmentFabric: Configuration = configurations.getByName("developmentFabric")
val projectVersion = version.toString()

configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    developmentFabric.extendsFrom(common)
}

dependencies {
    implementation(libs.fabric.loader)
    api(libs.fabric.api)
    runtimeOnly(project(":core"))

    common(project(":platform-modded-common")) {
        isTransitive = false
    }
    shadow(project(path = ":platform-modded-common", configuration = "transformProductionFabric")) {
        isTransitive = false
    }
    shadow(project(":core"))
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand("version" to projectVersion)
    }
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("shadow"))
    archiveFileName.set("hurricane-fabric-$projectVersion.jar")
    archiveClassifier.set("")
    from(rootProject.file("LICENSE"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
