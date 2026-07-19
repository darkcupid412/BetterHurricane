import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
}

allprojects {
    group = providers.gradleProperty("project_group").get()
    version = providers.gradleProperty("project_version").get()
}

subprojects {
    val moddedProjects = setOf(
        "platform-modded-common",
        "platform-fabric",
        "platform-neoforge"
    )
    val releaseVersion = if (name in moddedProjects) 25 else 17
    val compilerVersion = when {
        name in moddedProjects -> 25
        name == "platform-paper" -> 21
        else -> 17
    }

    if (name in moddedProjects) {
        apply(plugin = "hurricane.loom-conventions")
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(compilerVersion))
            withSourcesJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(releaseVersion)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
