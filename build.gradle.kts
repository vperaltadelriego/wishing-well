// Top-level build file where you can add configuration options common to all sub-projects/modules.
//
// This project currently has a single module (:app). If MetaMatch grows into a
// multi-module Clean Architecture setup (e.g. splitting :domain and :data into
// standalone Gradle modules, or adding a Kotlin Multiplatform :shared module),
// shared plugin versions are declared here once and applied per-module below.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Hilt's Gradle plugin patches the Android build to wire generated
    // dependency-injection code into the final APK.
    alias(libs.plugins.hilt.android) apply false
    // KSP (Kotlin Symbol Processing) is what actually reads Hilt's
    // @Module/@Inject annotations and generates the wiring code. It replaces
    // the older, slower "kapt" annotation processor.
    alias(libs.plugins.ksp) apply false
}
