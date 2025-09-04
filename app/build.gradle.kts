plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.nuwarning"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nuwarning"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") { // signingConfigs内ではcreateを使用する。
            storeFile = file("/nuwaKS.jks") // キーストアファイルのパス
            storePassword = "nuwanuwa" // キーストアのパスワード
            keyAlias = "nuwakey" // 鍵のエイリアス
            keyPassword = "nuwanuwa" // 鍵のパスワード
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata:2.5.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}