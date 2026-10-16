buildscript {
    dependencies {
        // Keeps the Kotlin compiler used by AGP's built-in Kotlin in sync with the Compose compiler plugin.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
