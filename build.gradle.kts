plugins {
    `kotlin-dsl`
 }

group = "software.onepiece.limits"
version = "0.0.1"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:1.4.21")
}

gradlePlugin {
    plugins {
        register("limits") {
            id = "software.onepiece.limits"
            implementationClass = "software.onepiece.limits.LimitsPlugin"
        }
    }
}
