import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import se.transmode.gradle.plugins.docker.DockerTask

buildscript {

    val gradleDockerVersion = "1.2"

    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {
        classpath("se.transmode.gradle:gradle-docker:$gradleDockerVersion")
    }
}

plugins {
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "2.0.2"
}

apply {
    plugin("docker")
}

application {
    mainClassName = "nl.endran.minireactor.playground.MainHub"
}

dependencies {
    compile(kotlin("stdlib"))
    compile(project(":minireactor-distributed"))
    compile(project(":minireactor-util"))

    testCompile("org.assertj:assertj-core:${ext["assertjVersion"]}")
    testCompile("org.jmockit:jmockit:${ext["jmockitVersion"]}")
    testCompile("junit:junit:${ext["junitVersion"]}")
}

val mainClass = "nl.endran.minireactor.playground.MainHub"
val appName = "MiniReactorHub"

application {
    mainClassName = mainClass
}


tasks.withType<Jar> {
    manifest {
        attributes(mapOf(Pair("Main-Class", mainClass)))
    }
}

tasks.withType<ShadowJar> {
    classifier = "fat"
}

tasks.withType<DockerTask> {
    maintainer = ext["dockerMaintainer"] as String
    baseImage = ext["dockerBaseImage"] as String
}

task("buildDocker", DockerTask::class) {
    push = false
    project.group = ext["dockerGroup"] as String
    applicationName = appName.toLowerCase()
    val shadowJar = tasks["shadowJar"] as ShadowJar
    addFile(shadowJar.archivePath.absolutePath)
    runCommand("sh -c 'touch /${shadowJar.archiveName}'")
    entryPoint(listOf("sh", "-c", "java -jar ${shadowJar.archiveName}"))

    doFirst {
        copy {
            from(jar)
            into(stageDir)
        }
    }
}

task("stoptDocker", Exec::class) {
    workingDir = File("../scripts")
    commandLine = listOf("./stop_docker.sh", appName.toLowerCase())
}

task("startDocker", Exec::class) {
    dependsOn.add(tasks["shadowJar"])

    workingDir = File("../scripts")
    commandLine = listOf("./start_docker.sh", (ext["dockerGroup"] as String).toLowerCase(), appName.toLowerCase(), ext["projectVersion"] as String)
}

task("pushDocker", Exec::class) {
    workingDir = File("../scripts")
    commandLine = listOf("./push_docker.sh", (ext["dockerGroup"] as String).toLowerCase(), appName.toLowerCase(), ext["projectVersion"] as String)
}

val build: DefaultTask by tasks
val jar = tasks["jar"] as Jar
val shadowJar = tasks["shadowJar"] as ShadowJar
val buildDocker = tasks["buildDocker"] as DockerTask
buildDocker.dependsOn(shadowJar)
build.dependsOn(shadowJar)
