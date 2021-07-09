plugins {
    java
    id("org.jetbrains.intellij") version "1.1.2"
    kotlin("jvm") version "1.4.32"
}

group = "com.ysj.idea.plugin.ycr"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2020.2")
    plugins.set(listOf("java", "org.jetbrains.kotlin"))
}

tasks {
    patchPluginXml {
        changeNotes.set(
            """
            """.trimIndent()
        )
    }
}