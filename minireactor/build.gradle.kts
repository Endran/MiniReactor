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
    compile("io.reactivex.rxjava2:rxjava:2.1.3")
    compile("io.reactivex.rxjava2:rxkotlin:2.2.0")

    testCompile("org.assertj:assertj-core:3.6.2")
    testCompile("org.jmockit:jmockit:1.31")
    testCompile("junit:junit:4.12")
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
