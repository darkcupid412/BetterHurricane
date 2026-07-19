plugins {
    `java-library`
    id("architectury-plugin")
    id("dev.architectury.loom-no-remap")
}

repositories {
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.neoforged.net/releases/")
}

architectury {
    minecraft = libs.versions.minecraft.get()
}

loom {
    silentMojangMappingsLicense()
}

dependencies {
    minecraft(libs.minecraft)
}
