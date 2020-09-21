plugins {
    `kotlin-dsl`
 }

group = "software.onepiece.limits"
version = "0.0.1"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
}

gradlePlugin {
    plugins {
        register("limits") {
            id = "software.onepiece.limits"
            implementationClass = "software.onepiece.limits.LimitsPlugin"
        }
    }
}
