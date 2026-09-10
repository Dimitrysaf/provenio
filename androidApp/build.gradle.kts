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
}

android {
    namespace = "io.github.dimitrysaf.provenio"
    compileSdk = 37

    // Every build has to be signed by the same key or Android treats an update as a
    // different app and refuses to install it. Falls back to the local auto-generated
    // debug key when the shared one is not checked out.
    val sharedDebugKeystore = file("debug.keystore")
    signingConfigs {
        getByName("debug") {
            if (sharedDebugKeystore.exists()) {
                storeFile = sharedDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    defaultConfig {
        applicationId = "io.github.dimitrysaf.provenio"
        // The torrent engine uses MethodHandle, which needs Android 8.0.
        minSdk = 26
        targetSdk = 37
        // Supplied by CI from the commit count so each build supersedes the last.
        versionCode = (findProperty("appVersionCode") as String?)?.toInt() ?: 1
        // CI overrides this with the prerelease version (see prerelease.yml); a local
        // build falls back to the global provenioVersion in the root gradle.properties.
        versionName = (findProperty("appVersionName") as String?)
            ?: (findProperty("provenioVersion") as String?)
            ?: "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
}
