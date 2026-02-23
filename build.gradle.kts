plugins {
    java
}

group = "cn.popcraft"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(16)
    }
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version
            )
        }
    }
    
    jar {
        archiveBaseName.set("IPChecker")
    }
}
