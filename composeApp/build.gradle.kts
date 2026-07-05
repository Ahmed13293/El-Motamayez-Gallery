import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Force coroutines version across all configurations to avoid
// conflicting Kotlin Wasm stdlib KLIBs from transitive deps
configurations.all {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.9.0",
            "org.jetbrains.kotlin:kotlin-stdlib:2.1.0",
            "org.jetbrains.kotlin:kotlin-stdlib-wasm-js:2.1.0"
        )
    }
    // Exclude the old Kotlin 1.9.x wasm stdlib artifact (renamed to kotlin-stdlib-wasm-js in Kotlin 2.x)
    // Prevents KLIB unique_name conflict when transitive deps still reference the old artifact
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-wasm")
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    jvm("desktop")
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting

        // Intermediate source set for all non-web platforms (Android + Desktop + iOS)
        // Voyager lives here because it has no wasmJs artifact
        val nonWebMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(nonWebMain)
        desktopMain.dependsOn(nonWebMain)
        iosMain.get().dependsOn(nonWebMain)

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.content.negotiation)

            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.kotlinx.datetime)
        }

        nonWebMain.dependencies {
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.tab)
            implementation(libs.voyager.koin)
            implementation(libs.voyager.transitions)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
            implementation(libs.itext7.core)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

android {
    namespace = "com.elmotamyez.gallery"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.elmotamyez.gallery"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.elmotamyez.gallery.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ElMotamyezGallery"
            packageVersion = "1.0.0"
        }
    }
}
