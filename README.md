# ONEPIECE Limits

Gradle plugin to generate immutable datastructures (Kotlin source code) with collections that contain a limited set of elements addressed by coordinates.
Also generates tools to diff data and (de)serialize such diffs. 

# How to use this plugin

Add an empty subproject to your Gradle build and apply this plugin in addition to `javba-library` and `org.jetbrains.kotlin.jvm` / `org.jetbrains.kotlin.multiplatform`.
You define the data model directly inside the `build.gradle.kts` file.
No other files need to be added to the project.
All source code is generated.
Use the generated classes in other suproejcts (or builds) by depending on the project to which you applied this plugin.

```
plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version "1.5.10" // or 'multiplatform'
    id("software.onepiece.limits") version "0.1"
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.1")
}

limits {
    basePackage.set("com.example.myapp")

    root("someData",
        coordinate("pageNumber", 12, "pn"),
        ...
    ) {
        ...
    }
}
```
