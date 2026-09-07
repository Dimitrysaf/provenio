import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }

    dependencies {
        implementation(projects.composeApp)
        implementation(libs.androidx.activity.compose)
    }
}

android {
    namespace = "io.github.dimitrysaf.provenio"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.dimitrysaf.provenio"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
}
