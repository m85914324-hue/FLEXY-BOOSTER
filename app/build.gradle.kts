plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// AdMob IDs come from gradle.properties. They default to GOOGLE'S OFFICIAL TEST IDs,
// so you can never accidentally click real ads while developing.
// Replace them in gradle.properties with your own IDs before publishing.
val admobAppId = providers.gradleProperty("ADMOB_APP_ID")
    .getOrElse("ca-app-pub-3940256099942544~3347511713")
val admobInterstitialId = providers.gradleProperty("ADMOB_INTERSTITIAL_ID")
    .getOrElse("ca-app-pub-3940256099942544/1033173712")
val admobRewardedId = providers.gradleProperty("ADMOB_REWARDED_ID")
    .getOrElse("ca-app-pub-3940256099942544/5224354917")
val plusProductId = providers.gradleProperty("PLUS_PRODUCT_ID")
    .getOrElse("flexy_plus")

android {
    namespace = "com.flexy.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flexy.app"
        minSdk = 26            // Android 8.0
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRewardedId\"")
        buildConfigField("String", "PLUS_PRODUCT_ID", "\"$plusProductId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Google's official ads SDK, consent (UMP) library, and Play Billing (for FLEXY PLUS)
    implementation("com.google.android.gms:play-services-ads:23.5.0")
    implementation("com.google.android.ump:user-messaging-platform:3.0.0")
    implementation("com.android.billingclient:billing:7.1.1")
}
