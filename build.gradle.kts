import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.31"

    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("org.springframework.boot") version "2.1.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    id("com.github.ben-manes.versions") version "0.21.0"
}

group = "org.bk"
version = "1.0-SNAPSHOT"

extra["junit-jupiter.version"] = "5.4.1"
extra["mockito.version"] = "2.27.0"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("net.dv8tion:JDA:3.8.3_463") {
        exclude(module = "opus-java")
    }
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation(group = "org.postgresql", name = "postgresql", version = "42.2.5")
    implementation("com.google.guava:guava:27.1-jre")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

configurations.all {
    exclude("org.springframework.boot", "spring-boot-starter-logging")
}

dependencyManagement {
    imports {
        mavenBom("org.apache.logging.log4j:log4j-bom:2.11.1")
    }
}
