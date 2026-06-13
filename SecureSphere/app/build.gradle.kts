plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // This connects to the Google Services file you added
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.securesphere"
    // specific version 34 is stable and safe (36 can cause errors)
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.securesphere"
        minSdk = 24
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.warrenstrange:googleauth:1.5.0")

    // --- FIREBASE SETUP ---
    // 1. The Bill of Materials (Keeps versions compatible)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // 2. Analytics (Good for stats)
    implementation("com.google.firebase:firebase-analytics")

    // 3. AUTHENTICATION (CRITICAL: Needed for Login/Signup)
    implementation("com.google.firebase:firebase-auth")

    //Internet Tools
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")

    //password manager
    implementation("androidx.biometric:biometric:1.1.0")

    //caller identify
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    dependencies {
        // 1. Keep location and firebase tools
        implementation("com.google.android.gms:play-services-location:21.2.0")
        implementation("com.google.firebase:firebase-database-ktx:20.3.1")

        // 2. Add the 100% Free OpenStreetMap library
        implementation("org.osmdroid:osmdroid-android:6.1.18")
    }

    android {
        // ... your existing config (compileSdk, defaultConfig, etc.) ...

        packaging {
            resources {
                excludes += "/META-INF/DEPENDENCIES"
                excludes += "/META-INF/LICENSE"
                excludes += "/META-INF/NOTICE"
            }
        }
    }
}