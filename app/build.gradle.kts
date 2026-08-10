plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ng.wimika.samplebankapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "ng.wimika.samplebankapp"
        minSdk = 29
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val sabiBankBaseUrl = providers.gradleProperty("SABI_BANK_BASE_URL")
            .orElse(providers.environmentVariable("SABI_BANK_BASE_URL"))
            .orElse("http://10.0.2.2:5103/")
            .get()
        buildConfigField("String", "SABI_BANK_BASE_URL", "\"$sabiBankBaseUrl\"")
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // No release keystore in this repo; debug-sign so local release
            // builds are installable for testing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(files("libs/moneyguard-sdk-release.aar"))
    implementation(files("libs/moneyguard-sdk-commons-release.aar"))
    implementation(files("libs/moneyguard-sdk-auth-release.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    implementation(libs.okhttp3)
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.okhttp3.logging)
    implementation(libs.gson.converter)


    implementation("joda-time:joda-time:2.12.5")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation(libs.coil.compose)
}
