
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = java.net.URI.create(System.getenv("MAVEN_REGISTRY_SNAPSHOTS"))
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_PASSWORD")
            }
        }
        maven {
            url = java.net.URI.create(System.getenv("MAVEN_REGISTRY_RELEASES"))
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_PASSWORD")
            }
        }
    }
}

rootProject.name = "openapi"
