import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

// Signing credentials come only from local.properties — no env var fallback.
fun signingProperty(localKey: String): String? =
    localProperties.getProperty(localKey)?.takeIf { it.isNotBlank() }

android {
    namespace = "dev.fitiavana.learning_mgmt"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.fitiavana.learning_mgmt"
        minSdk = 26
        targetSdk = 37
        versionCode = 7
        versionName = "0.7"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingProperty("android.keystore.path")
            val storePwd = signingProperty("android.keystore.password")
            val keyAliasProp = signingProperty("android.key.alias")
            val keyPwd = signingProperty("android.key.password")
            if (storeFilePath != null && storePwd != null && keyAliasProp != null && keyPwd != null) {
                storeFile = file(storeFilePath)
                storePassword = storePwd
                keyAlias = keyAliasProp
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        // Exported Room schemas, read by the migration tests (Robolectric merges the debug assets).
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Robolectric reaches into JDK internals, which JDK 17+ encapsulates by default.
            all { test ->
                test.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Compose's UI test pulls an Espresso too old for the SDK Robolectric runs on.
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}