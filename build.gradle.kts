plugins {
    kotlin("jvm") version "2.0.0"
    id("java")
    application
    kotlin("plugin.serialization") version "2.0.0"
}

group = "com.github.abyssnlp"
version = "0.2"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.github.abyssnlp.MainKt")
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(
            listOf(
                "compileJava",
                "compileKotlin",
                "processResources"
            )
        )
        archiveClassifier.set("standalone")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) }
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar)
    }
}

dependencies {
    implementation(libs.bundles.allCore)
    implementation(libs.bundles.monitoring)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.allTests)
}

task("getVersion") {
    group = "custom"
    doLast {
        println(project.version)
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}