plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

application {
    mainClass.set("Ankist")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20240303")
}
