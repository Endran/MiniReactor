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
    val shadowJar = tasks["shadowJar"] as ShadowJar
    dependsOn(shadowJar)

    push = false
    project.group = ext["dockerGroup"] as String
    applicationName = appName.toLowerCase()
    addFile(shadowJar.archivePath.absolutePath)
    runCommand("sh -c 'touch /${shadowJar.archiveName}'")
    entryPoint(listOf("sh", "-c", "java -jar ${shadowJar.archiveName}"))

    doFirst {
        copy {
            from(tasks["jar"] as Jar)
            into(stageDir)
        }
    }
}

task("stopDocker", Exec::class) {
    workingDir = File("../scripts")
    commandLine = listOf("./stop_docker.sh", appName.toLowerCase())
}

task("startDocker", Exec::class) {
    dependsOn.add(tasks["stopDocker"])

    workingDir = File("../scripts")
    commandLine = listOf("./start_docker.sh", (ext["dockerGroup"] as String).toLowerCase(), appName.toLowerCase(), ext["projectVersion"] as String)
}

task("pushDocker", Exec::class) {
    workingDir = File("../scripts")
    commandLine = listOf("./push_docker.sh", (ext["dockerGroup"] as String).toLowerCase(), appName.toLowerCase(), ext["projectVersion"] as String)
}

val build: DefaultTask by tasks
val shadowJar = tasks["shadowJar"] as ShadowJar
build.dependsOn(shadowJar)
