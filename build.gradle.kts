plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cn.popcraft"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version
            )
        }
    }
    
    shadowJar {
        archiveClassifier.set("")
        relocate("org.sqlite", "cn.popcraft.ipchecker.libs.sqlite")
        relocate("org.slf4j", "cn.popcraft.ipchecker.libs.slf4j")
    }

    jar {
        archiveBaseName.set("IPChecker")
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}
