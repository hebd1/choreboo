// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.gradle.play.publisher) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}

// Task to compile Data Connect schemas and generate the Kotlin SDK
tasks {
    register<Exec>("dataconnectCompile") {
        workingDir = project.file("./dataconnect")
        if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(
                org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
            )
        ) {
            commandLine("npx.cmd", "-y", "firebase-tools@latest", "dataconnect:compile")
        } else {
            commandLine("npx", "-y", "firebase-tools@latest", "dataconnect:compile")
        }
        isIgnoreExitValue = false
    }
}