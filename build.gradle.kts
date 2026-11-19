// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // GeckoView 160+ Kotlin 2.4 ile derlendi; AGP'nin yerleşik Kotlin'ini (2.2) geçersiz kılar.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.aboutlibraries) apply false
}
