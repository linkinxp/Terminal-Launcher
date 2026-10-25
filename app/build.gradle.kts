import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.github.takahirom.roborazzi")
}
// Upload-key signing for the Play bundle: read from a properties file outside the repository (absent on a dev machine without it).
val signingProps = Properties()
File("/root/.signing/keystore.properties").takeIf { it.exists() }?.inputStream()?.use { signingProps.load(it) }

android {
    namespace = "dev.linkinxp.termlauncher"
    compileSdk = 37
    defaultConfig {
        // The Play Store app. The debug build keeps the old id (see androidComponents below) so a phone running it is not disturbed.
        applicationId = "com.linkinxp.terminal"
        // 34+: androidx.graphics.path then uses the framework PathIterator, so its native lib (not 16 KB
        // aligned, even in 1.1.0) is never loaded and is left out of the APK.
        minSdk = 34
        targetSdk = 36
        versionCode = 104
        versionName = "1.0.4"
    }
    packaging { jniLibs { excludes += "**/libandroidx.graphics.path.so" } }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        if (signingProps.containsKey("storeFile")) create("upload") {
            storeFile = File(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug { buildConfigField("boolean", "LOCK_SERVICE", "true") }
        release {
            // No accessibility service in the store build (src/release/AndroidManifest.xml removes it).
            buildConfigField("boolean", "LOCK_SERVICE", "false")
            isMinifyEnabled = false
            signingConfigs.findByName("upload")?.let { signingConfig = it }
        }
    }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}
androidComponents {
    onVariants { variant -> if (variant.buildType == "debug") variant.applicationId.set("dev.linkinxp.termlauncher") }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    // Pinned above the version Compose pulls in: 1.0.1 ships a native lib whose RELRO is not 16 KB aligned.
    implementation("androidx.graphics:graphics-path:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    // Draws the generated SVG wallpapers (Apache 2.0, ~200 KB).
    implementation("com.caverock:androidsvg-aar:1.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    // JVM screenshots of the Compose screens (no emulator)
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.77.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.77.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test:core:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
