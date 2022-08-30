plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.0.0"
 }

group = "software.onepiece.limits"
version = "0.2"

repositories {
    gradlePluginPortal()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

dependencies {
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:[1.4.0,1.5.10]")
}

gradlePlugin {
    plugins {
        register("onepiece-limits") {
            id = "software.onepiece.limits"
            implementationClass = "software.onepiece.limits.LimitsPlugin"
            displayName = "Generate immutable data structures as Kotlin sources"
            description = "Plugin to generate immutable data structures (Kotlin source code) with collections that contain a limited set of elements addressed by coordinates. Also generates tools to diff data and (de)serialize such diffs."
        }
    }
}

pluginBundle {
    website = "https://github.com/jjohannes/onepiece-limits"
    vcsUrl = "https://github.com/jjohannes/onepiece-limits.git"
    tags = listOf("kotlin", "data model", "domain model")
}
