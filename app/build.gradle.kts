import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rajpawardotin.kosh"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.rajpawardotin.kosh"
        minSdk = 36
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val stream = FileInputStream(localPropertiesFile)
        localProperties.load(stream)
        stream.close()
    }

    signingConfigs {
        create("release") {
            val keystorePath = localProperties.getProperty("RELEASE_KEYSTORE_FILE")
            val keystorePassword = localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
            val keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
            val keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")

            if (keystorePath != null && keystorePath != "" &&
                keystorePassword != null && keystorePassword != "" &&
                keyAlias != null && keyAlias != "" &&
                keyPassword != null && keyPassword != ""
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            
            ndk {
                debugSymbolLevel = "full"
            }
            
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
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
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            // Enable legacy packaging (extract libraries to nativeLibraryDir) to allow the
            // Qualcomm QNN NPU delegate runtime to locate and dynamically load its driver stubs/skels.
            useLegacyPackaging = true
            pickFirsts.add("**/libLiteRt*.so")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.markdown.renderer)
    implementation(libs.coil.compose)
    implementation(libs.jsoup)
    implementation(libs.okhttp)
    implementation(libs.pdfbox.android)

    // implementation(libs.mlkit.genai.prompt)
    implementation(libs.litertlm.android)
    implementation(libs.litert.core)
    implementation(libs.litert.gpu)
    implementation(libs.qnn.litert.delegate)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}