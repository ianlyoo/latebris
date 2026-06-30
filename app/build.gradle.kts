plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val openCellIdApiKey = providers.gradleProperty("OPEN_CELL_ID_API_KEY")
    .orElse(providers.environmentVariable("OPEN_CELL_ID_API_KEY"))
    .orElse("")
    .get()

val escapedOpenCellIdApiKey = openCellIdApiKey
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val signingStoreFile = providers.environmentVariable("ANDROID_SIGNING_STORE_FILE")
val signingStorePassword = providers.environmentVariable("ANDROID_SIGNING_STORE_PASSWORD")
val signingKeyAlias = providers.environmentVariable("ANDROID_SIGNING_KEY_ALIAS")
val signingKeyPassword = providers.environmentVariable("ANDROID_SIGNING_KEY_PASSWORD")
val hasReleaseSigning = signingStoreFile.isPresent &&
    signingStorePassword.isPresent &&
    signingKeyAlias.isPresent &&
    signingKeyPassword.isPresent

android {
    namespace = "com.example.gpstick"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gpstick"
        minSdk = 31
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
        buildConfigField("String", "OPEN_CELL_ID_API_KEY", "\"$escapedOpenCellIdApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(signingStoreFile.get())
                storePassword = signingStorePassword.get()
                keyAlias = signingKeyAlias.get()
                keyPassword = signingKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.google.material)
    implementation(libs.play.services.location)
    implementation(libs.google.gson)
    compileOnly(project(":xposed-stubs"))

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
