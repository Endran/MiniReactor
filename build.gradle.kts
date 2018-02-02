import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient.compile
import org.jetbrains.kotlin.load.kotlin.isContainedByCompiledPartOfOurModule
import java.net.URI

plugins {
    base
    kotlin("jvm") version "1.1.51" apply false
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
        kotlin("jvm") version "1.1.51" apply false
    }

    group = "com.github.Endran"
    version = "0.2.1"

    dependencies {
        // This I really want...
        // ... and also variables...
        // ... and version updates
    }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}