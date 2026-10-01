plugins {
    kotlin("jvm") version "2.4.20"
    application
}

group = "ru.sobes"
version = "0.1.0-SNAPSHOT"
description = "Sobes Telegram bot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.+")
    implementation("org.slf4j:slf4j-simple:2.0.+")
    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

application {
    mainClass.set("ru.sobes.bot.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
