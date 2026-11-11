plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}
android {
    namespace = "com.lavaflow.puzzle"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.lavaflow.puzzle"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.3.1"
        testInstrumentationRunner = "com.lavaflow.puzzle.GateInstrumentation"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("long", "INITIAL_COINS", "2000L")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            val initialCoins = providers.gradleProperty("productionInitialCoins").orElse("0").get().toLong()
            require(initialCoins >= 0)
            buildConfigField("long", "INITIAL_COINS", "${initialCoins}L")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("delivery") {
            initWith(getByName("release"))
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
            buildConfigField("long", "INITIAL_COINS", "2000L")
        }
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
}
