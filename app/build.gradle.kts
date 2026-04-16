import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
    alias(libs.plugins.gradle.play.publisher)
}

// Load signing credentials from local.properties (not checked in) or environment variables.
// local.properties keys: KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
// Fallback: environment variables of the same names (for CI).
val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}
fun prop(name: String): String? = localProps.getProperty(name) ?: System.getenv(name)

android {
    namespace = "com.choreboo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.choreboo.app"
        minSdk = 24
        targetSdk = 36
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 2
        versionName = (project.findProperty("versionName") as String?) ?: "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = prop("KEYSTORE_PATH")
            val keystorePassword = prop("KEYSTORE_PASSWORD")
            val keyAlias = prop("KEY_ALIAS")
            val keyPassword = prop("KEY_PASSWORD")
            if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Use Google's public test AdMob App ID in debug builds
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~1033173712"
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            // Use production AdMob App ID in release builds
            manifestPlaceholders["admobAppId"] = "ca-app-pub-5747107953663953~4868802037"
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Add Data Connect generated SDK sources
    sourceSets.getByName("main") {
        kotlin.srcDir("build/generated/sources")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Google Play Publisher (Triple-T) — uploads AAB to Play Console via GitHub Actions.
// Authentication uses Workload Identity Federation; GOOGLE_APPLICATION_CREDENTIALS is set
// by the google-github-actions/auth step in the workflow.
play {
    track.set("internal")
    val credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if (credPath != null) {
        serviceAccountCredentials.set(file(credPath))
    } else {
        serviceAccountCredentials.set(file("non-existent-placeholder.json"))
    }
    defaultToAppBundles.set(true)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.dataconnect)
    implementation(libs.firebase.storage)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp("androidx.hilt:hilt-compiler:1.2.0") // required for @HiltWorker code generation
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Glance (Widget)
    implementation(libs.glance)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Material Icons Extended
    implementation(libs.material.icons.extended)

    // Gson
    implementation(libs.gson)

    // Splash Screen
    implementation(libs.splashscreen)

    // Emoji Picker
    implementation(libs.emoji.picker)

    // Google Play Billing
    implementation(libs.billing.ktx)

    // AdMob
    implementation(libs.play.services.ads)

    // Timber
    implementation(libs.timber)

    // Desugaring (java.time support for minSdk 24)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}