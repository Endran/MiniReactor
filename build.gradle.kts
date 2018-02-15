import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient.compile
import org.jetbrains.kotlin.load.kotlin.isContainedByCompiledPartOfOurModule
import java.net.URI
import org.gradle.plugins.ide.idea.model.IdeaModule

buildscript {

    val gradleVersionsVersion = "0.17.0"
    val kotlinVersion = "1.2.21"

    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:$gradleVersionsVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

apply {
    plugin("com.github.ben-manes.versions")
}

plugins {
    base
    idea
    java
}

repositories {
    jcenter()
}

configure<IdeaModel> {
    project {
        languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
    }
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        inheritOutputDirs = false
        outputDir = file("$buildDir/classes/main/")
    }
}

allprojects {group = "com.github.Endran"
    version = "0.3.0"
    ext["projectVersion"] = version

    ext["dockerGroup"] = "registry.gitlab.com/endran/playground"
    ext["dockerMaintainer"] = "David Hardy <davidhardy85@gmail.events>"
    ext["dockerBaseImage"] = "frolvlad/alpine-oraclejdk8:slim"

    ext["assertjVersion"] = "3.9.0"
    ext["gsonVersion"] = "2.8.2"
    ext["rxJavaVersion"] = "2.1.9"
    ext["rxKotlinVersion"] = "2.2.0"
    ext["jmockitVersion"] = "1.38"
    ext["junitVersion"] = "4.12"
    ext["socketOutletVersion"] = "2ef0b591a3"
}

subprojects {

    apply {
        plugin("kotlin")
    }

    repositories {
        jcenter()
        maven { setUrl("https://jitpack.io") }
    }

    plugins {
        kotlin("jvm") version ext["kotlinVersion"] as String apply false
    }

    dependencies {
        testCompile("org.assertj:assertj-core:${ext["assertjVersion"]}")
        testCompile("org.jmockit:jmockit:${ext["jmockitVersion"]}")
        testCompile("junit:junit:${ext["junitVersion"]}")
    }
}

dependencies {
    subprojects.forEach {
        archives(it)
    }
}
