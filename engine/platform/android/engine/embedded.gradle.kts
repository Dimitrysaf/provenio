// Builds the engine as the `:engine` module of the Provenio app build, so a change to the native
// core, the JNI bridge or the Kotlin wrapper is compiled into the next app build without a
// separate engine release. The standalone build in build.gradle.kts, which the Engine workflow
// runs to publish the AAR, stays as it is; this file mirrors its native setup for the app's
// Android Gradle Plugin.

import com.android.build.api.dsl.LibraryExtension
import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary)
}

val opensslVersion = "3.5.7"
val androidNdkVersion = "29.0.14206865"
val allAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val engineRepository = layout.projectDirectory.dir("../../..")
val opensslOutput = layout.projectDirectory.dir("../.deps/openssl-$opensslVersion")
val generatedLicenseResources = layout.buildDirectory.dir("generated/licenseResources")

// The standalone build owns build/; keep this build's output beside it rather than mixed in.
layout.buildDirectory.set(layout.projectDirectory.dir("build/embedded"))

// Each ABI compiles OpenSSL and libtorrent from scratch the first time, so a machine that only
// ever installs on one kind of device can list just that ABI:
//   provenio.engine.abis=arm64-v8a
// in local.properties or as a Gradle property. Every ABI is built when it is not set.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val engineAbis = (
    providers.gradleProperty("provenio.engine.abis").orNull
        ?: localProperties.getProperty("provenio.engine.abis")
    )
    ?.split(',')
    ?.map(String::trim)
    ?.filter(String::isNotEmpty)
    ?.also { abis ->
        val unknown = abis - allAbis.toSet()
        require(unknown.isEmpty()) { "provenio.engine.abis lists unknown ABIs: $unknown" }
    }
    ?.takeIf { it.isNotEmpty() }
    ?: allAbis

// libtorrent and Boost translation units take 1–2 GB each to compile, and Ninja would otherwise
// run one per core plus two: enough to exhaust a desktop's memory. Override with
// provenio.engine.jobs in local.properties or as a Gradle property.
val engineCompileJobs = (
    providers.gradleProperty("provenio.engine.jobs").orNull
        ?: localProperties.getProperty("provenio.engine.jobs")
    )?.toIntOrNull()?.coerceAtLeast(1) ?: 2

extensions.configure<LibraryExtension> {
    namespace = "com.engine"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = androidNdkVersion

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters += engineAbis
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_static",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DENGINE_ENABLE_LIBTORRENT=ON",
                    "-DENGINE_BUILD_SHARED=ON",
                    "-DENGINE_BUILD_TESTS=OFF",
                    "-DCMAKE_JOB_POOLS=compile=$engineCompileJobs;link=1",
                    "-DCMAKE_JOB_POOL_COMPILE=compile",
                    "-DCMAKE_JOB_POOL_LINK=link",
                )
                targets += "engine"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }

    buildTypes {
        debug {
            isJniDebuggable = true
        }
    }

    // Matches the app modules that consume it.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets.getByName("main").resources.directories.add(
        generatedLicenseResources.get().asFile.path,
    )
}

val ndkDirectory = androidComponents.sdkComponents.ndkDirectory

val prepareAndroidOpenSsl = tasks.register<Exec>("prepareAndroidOpenSsl") {
    group = "build setup"
    description = "Builds pinned static OpenSSL libraries for the engine's Android ABIs"
    inputs.file(engineRepository.file("scripts/build-android-openssl.sh"))
    inputs.property("opensslVersion", opensslVersion)
    inputs.property("abis", engineAbis)
    outputs.dirs(engineAbis.map { abi -> opensslOutput.dir("install/$abi") })
    environment("ENGINE_ANDROID_ABIS", engineAbis.joinToString(" "))
    // The script skips an ABI whose install is already complete, so this is cheap after the
    // first build. Locals only below: the configuration cache cannot store the script itself.
    val ndk = ndkDirectory
    val output = opensslOutput.asFile.absolutePath
    executable = engineRepository.file("scripts/build-android-openssl.sh").asFile.absolutePath
    argumentProviders.add(CommandLineArgumentProvider { listOf(ndk.get().asFile.absolutePath, output) })
}

val prepareAndroidLicenseResources = tasks.register<Sync>("prepareAndroidLicenseResources") {
    dependsOn(prepareAndroidOpenSsl)
    from(engineRepository.file("LICENSE")) {
        into("META-INF")
        rename { "ENGINE_LICENSE.txt" }
    }
    from(engineRepository.file("THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
        rename { "ENGINE_THIRD_PARTY_NOTICES.md" }
    }
    from(opensslOutput.file("src/openssl-$opensslVersion/LICENSE.txt")) {
        into("META-INF")
        rename { "OPENSSL_LICENSE.txt" }
    }
    from(opensslOutput.file("downloads/libtorrent-LICENSE.txt")) {
        into("META-INF")
        rename { "LIBTORRENT_LICENSE.txt" }
    }
    from(opensslOutput.file("downloads/try_signal-LICENSE.txt")) {
        into("META-INF")
        rename { "TRY_SIGNAL_LICENSE.txt" }
    }
    from(opensslOutput.file("downloads/boost-LICENSE_1_0.txt")) {
        into("META-INF")
        rename { "BOOST_LICENSE_1_0.txt" }
    }
    into(generatedLicenseResources)
}

tasks.configureEach {
    if (name.startsWith("configureCMake")) {
        dependsOn(prepareAndroidOpenSsl)
    }
    if (name.startsWith("process") && name.endsWith("JavaRes")) {
        dependsOn(prepareAndroidLicenseResources)
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.test)
}
