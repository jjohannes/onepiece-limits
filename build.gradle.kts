plugins {
    `kotlin-dsl`
 }

group = "software.onepiece"
version = "0.0.1"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:1.3.0")
}

//gradlePlugin {
//    plugins {
//        register("limits") {
//            id = "software.onepiece.limits"
//            implementationClass = "software.onepiece.limits.LimitsPlugin"
//        }
//    }
//}
