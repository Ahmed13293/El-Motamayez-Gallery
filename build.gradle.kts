plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.kotlin.serialization).apply(false)
    alias(libs.plugins.android.application).apply(false)
    id("com.google.gms.google-services") version "4.4.2" apply false
}
