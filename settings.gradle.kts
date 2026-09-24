rootProject.name = "Provenio"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":androidApp")

// Local builds compile the P2P engine from engine/, so a change there is in the next app build.
// CI keeps linking the binary the Engine workflow commits to composeApp/libs, which spares every
// app build the NDK toolchain. -Pprovenio.engine.fromSource=true|false overrides either way.
val engineFromSource = providers.gradleProperty("provenio.engine.fromSource").orNull?.toBooleanStrict()
    ?: (providers.environmentVariable("CI").orNull == null)
if (engineFromSource) {
    include(":engine")
    project(":engine").apply {
        projectDir = file("engine/platform/android/engine")
        buildFileName = "embedded.gradle.kts"
    }
}
