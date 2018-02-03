import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories

plugins {
    application
    kotlin("jvm")
}

application {
    mainClassName = "cli.Main"
}

dependencies {
    compile(kotlin("stdlib"))
    compile(project(":minireactor-plant"))

    testCompile("org.assertj:assertj-core:${ext["assertjVersion"]}")
    testCompile("org.jmockit:jmockit:${ext["jmockitVersion"]}")
    testCompile("junit:junit:${ext["junitVersion"]}")
}
