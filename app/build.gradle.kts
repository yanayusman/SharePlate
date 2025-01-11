plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.shareplate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.shareplate"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

}
dependencies {
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // Gemini API
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Google Play Services - Auth and Identity
    implementation("com.google.android.gms:play-services-auth:21.3.0") // Match version with Credential Manager
    implementation("com.google.android.gms:play-services-identity:18.1.0")

    // Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    // Other Core Dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("androidx.activity:activity-ktx:1.9.3") // Kotlin extensions for Activity

    // Google Identity Services
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.libraries.places:places:2.7.0")
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.places)

    // Other Libraries
    implementation("com.makeramen:roundedimageview:2.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")

    // Google Play Services Tasks (ensure latest)
    implementation("com.google.android.gms:play-services-tasks:18.2.0")


    // Firebase Auth and Google Play Services
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Add Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-storage:21.0.1")

    // Add Glide for image loading (optional but recommended)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // LocalBroadcastManager
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    implementation("com.google.firebase:firebase-messaging:24.1.0")

    // Google Play Service Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.libraries.places:places:3.3.0")

    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("org.json:json:20210307")
    // Other dependencies

    // Markdown Support
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("io.noties.markwon:image:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")

    // Facebook SDK
    implementation("com.facebook.android:facebook-login:latest.release")
    implementation("com.facebook.android:facebook-android-sdk:latest.release")
}


