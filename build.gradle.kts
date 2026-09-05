plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20240303")
}
