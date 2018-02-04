import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.dokka.gradle.DokkaTask

buildscript {

    repositories {
        jcenter()
        mavenCentral()
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    }

    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.16-eap-3")
    }
}

plugins {
    kotlin("jvm")
}

apply {
    plugin("maven")
    plugin("org.jetbrains.dokka")
}

dependencies {
    compile(project(":minireactor-core"))
    compile("com.github.endran.SocketOutlet:client:${ext["socketOutletVersion"]}")
    compile("com.github.endran.SocketOutlet:server:${ext["socketOutletVersion"]}")

    compile("com.google.code.gson:gson:${ext["gsonVersion"]}")

    testCompile("org.assertj:assertj-core:${ext["assertjVersion"]}")
    testCompile("org.jmockit:jmockit:${ext["jmockitVersion"]}")
    testCompile("junit:junit:${ext["junitVersion"]}")
}

val dokkaJar = task<Jar>("dokkaJar") {
    dependsOn("dokka")
    classifier = "javadoc"
    from((tasks.getByName("dokka") as DokkaTask).outputDirectory)
}

val sourcesJar = task<Jar>("sourcesJar") {
    classifier = "sources"
    from(the<JavaPluginConvention>().sourceSets.getByName("main").allSource)
}

artifacts.add("archives", dokkaJar)
artifacts.add("archives", sourcesJar)
