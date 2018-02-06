import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient.compile
import org.jetbrains.kotlin.load.kotlin.isContainedByCompiledPartOfOurModule
import java.net.URI

buildscript {

    val gradleVersionsVersion = "0.17.0"

    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:$gradleVersionsVersion")
    }
}

apply {
    plugin("com.github.ben-manes.versions")
}

plugins {
    base
    idea
    kotlin("jvm") version "1.2.21" apply false
}

repositories {
    jcenter()
}

subprojects {

    repositories {
        jcenter()
        maven { setUrl("https://jitpack.io") }
    }

    plugins {
        kotlin("jvm") version "1.2.21" apply false
    }

    group = "com.github.Endran"
    version = "0.3.0"

    ext["assertjVersion"] = "3.9.0"
    ext["gsonVersion"] = "2.8.2"
    ext["rxJavaVersion"] = "2.1.9"
    ext["rxKotlinVersion"] = "2.2.0"
    ext["jmockitVersion"] = "1.38"
    ext["junitVersion"] = "4.12"
    ext["socketOutletVersion"] = "aec78e4456"

    dependencies {
        // This I really want...
        // ... and version updates
    }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}
