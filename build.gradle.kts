plugins {
    id("java")

    id("xyz.jpenilla.run-velocity") version "2.1.0"
}

group = "org.sbst"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    compileOnly("net.kyori:adventure-text-minimessage:4.14.0")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")
}

tasks {
    runVelocity {
        velocityVersion("3.2.0-SNAPSHOT")
    }
}