plugins {
    kotlin("jvm") version "1.2.21"
    `maven-publish`
}

group = "software.onepiece.limits"
version = "0.0.1"

repositories {
    gradlePluginPortal()
    mavenCentral()
}
dependencies {
    compileOnly(gradleApi())
    compile(kotlin("stdlib-jre8", "1.2.21"))
    compile("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:1.2.21")
}
