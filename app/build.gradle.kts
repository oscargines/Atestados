
import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.oscar.atestados"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.oscar.atestados"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["nfcPermission"] = "android.permission.NFC"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["nfcPermission"] = "android.permission.NFC"
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
    packaging {
        resources {
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "AndroidManifest.xml",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11" // Compatible con Kotlin 1.9.22
    }
    buildToolsVersion = "35.0.0"
}

dependencies {
    // Core y utilidades
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Serialización
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    // Localización
    implementation (libs.play.services.location)

    // Splashscreen
    implementation(libs.androidx.core.splashscreen)

    // Cámara
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.core) // ZXing lector qr
    implementation (libs.barcode.scanning)
    implementation (libs.barcode.scanning.v1720)

    // ML Kit y visión
    implementation(libs.vision.common)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.play.services.mlkit.text.recognition)

    // Bluetooth
    implementation(libs.androidx.bluetooth)

    // NFC y DNIeDroid
    implementation(files("libs/dniedroid-release.aar"))
    implementation("org.bouncycastle:bcprov-jdk18on:1.74")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.74")
    implementation("org.bouncycastle:bcutil-jdk18on:1.74")
    implementation(libs.jmrtd)
    implementation(libs.scuba.smartcards)

    // Gson
    implementation(libs.gson)

    // Coil para imágenes
    implementation(libs.coil.compose)
    implementation(libs.androidx.runtime.livedata)

    // Zebra
    implementation(files("libs/ZSDK_ANDROID_API.jar"))

    // PDF
    implementation(libs.itext.core)
    implementation(libs.layout)
    implementation(libs.commonmark)  // Parsear Markdown
    implementation(libs.html2pdf)
    implementation (libs.slf4j.simple)
    implementation(libs.androidx.room.ktx)
    implementation(libs.play.services.mlkit.barcode.scanning) // Proveedor de logging para SLF4J

    // Pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Forzar versión 1.78 para BouncyCastle
configurations.all {
    resolutionStrategy {
        force("org.bouncycastle:bcprov-jdk18on:1.74")
        force ("org.bouncycastle:bcpkix-jdk18on:1.74")
        force ("org.bouncycastle:bcutil-jdk18on:1.74")
    }
}