plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.2.2")
    implementation("org.apache.commons:commons-compress:1.28.0")
}
