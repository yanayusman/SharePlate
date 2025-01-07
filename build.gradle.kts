// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories{
        google()
        mavenCentral()
    }
    dependencies {
        // Add the Google Services plugin
        classpath (libs.google.services)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
}

