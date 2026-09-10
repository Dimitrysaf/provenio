import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "io.github.dimitrysaf.provenio.shared"
        compileSdk = 37

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    sourceSets {
        // The app deliberately targets Material 3 Expressive; opting in once here beats
        // annotating every file that touches an expressive component.
        all {
            languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
            languageSettings.optIn("androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.navigation.compose)
            implementation(libs.sqldelight.runtime)
            implementation(libs.compose.settings.expressive)
            implementation(libs.compose.adaptive)
            implementation(libs.compose.adaptive.layout)
            implementation(libs.compose.adaptive.navigation)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        // Both targets are JVM, and the torrent engine and HTTP server are JVM only.
        // A shared intermediate source set lets them be written once.
        val jvmShared by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.bt.core)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
            }
        }
        androidMain.get().dependsOn(jvmShared)

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.ui)
            implementation(libs.nextlib.media3ext)
            implementation(libs.sqldelight.android.driver)
        }
        val desktopMain by getting
        desktopMain.dependsOn(jvmShared)
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

sqldelight {
    databases {
        create("ProvenioDatabase") {
            packageName.set("io.github.dimitrysaf.provenio.db")
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "Provenio"
            // CI overrides this with the prerelease version (see prerelease.yml); a local
            // build falls back to the global provenioVersion in the root gradle.properties.
            packageVersion = (findProperty("appVersionName") as String?)
                ?: (findProperty("provenioVersion") as String?)
                ?: "1.0.0"
        }
    }
}
