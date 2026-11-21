plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// A separate app that reads location, Wi-Fi and cell data the way any third-party
// app would. It declares no <queries> entry, so it cannot see Latebris itself.
// Two flavors with different application ids: one goes into the LSPosed scope,
// the other stays out of it as the control.
android {
    namespace = "io.github.ianlyoo.latebris.probe"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.ianlyoo.latebris.probe"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "scope"
    productFlavors {
        create("scoped") {
            dimension = "scope"
            applicationIdSuffix = ".scoped"
            resValue("string", "probe_label", "Latebris probe (scoped)")
        }
        create("unscoped") {
            dimension = "scope"
            applicationIdSuffix = ".unscoped"
            resValue("string", "probe_label", "Latebris probe (unscoped)")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.play.services.location)
}
