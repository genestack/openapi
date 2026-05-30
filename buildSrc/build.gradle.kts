plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.1.4")
    implementation("org.apache.commons:commons-compress:1.28.0")
}
