import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

// App version (versionCode / versionName), see version.properties
val versionProperties =
    Properties().apply {
      val file = rootProject.file("version.properties")
      require(file.isFile) { "version.properties is missing in the project root" }
      file.inputStream().use { load(it) }
    }
val appVersionCode: Int =
    versionProperties.getProperty("versionCode")?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        ?: error("version.properties: versionCode must be a positive whole number")
val appVersionName: String =
    versionProperties.getProperty("versionName")?.trim()?.takeIf { it.isNotEmpty() }
        ?: error("version.properties: versionName is missing")

// Release signing settings, kept out of version control
val keystoreProperties =
    Properties().apply {
      val file = rootProject.file("keystore.properties")
      if (file.isFile) file.inputStream().use { load(it) }
    }

/** A keystore.properties value, else the environment variable. */
fun signingValue(property: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(property)?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv(environmentVariable)?.takeIf { it.isNotEmpty() }

/** Keystore path; "~" at the start means the home directory. */
fun keystorePath(): String? =
    signingValue("storeFile", "KEYSTORE_PATH")
        ?.replaceFirst(Regex("^~(?=/|$)"), System.getProperty("user.home"))

android {
  namespace = "org.librecellar"
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "org.librecellar"
    minSdk = 26
    targetSdk = 37
    versionCode = appVersionCode
    versionName = appVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    // Release signing reads keystore.properties in the project root (git-ignored, see
    // keystore.properties.example) and falls back to the environment variables KEYSTORE_PATH,
    // KEY_ALIAS, STORE_PASSWORD and KEY_PASSWORD (e.g. for CI).
    create("release") {
      storeFile = file(keystorePath() ?: "${rootDir}/my-upload-key.jks")
      storePassword = signingValue("storePassword", "STORE_PASSWORD")
      keyAlias = signingValue("keyAlias", "KEY_ALIAS") ?: "upload"
      keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // Barcode scanning (camera preview + ML Kit)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)
  // Bottle pictures
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)
  // Backend REST API
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.kotlinx.serialization)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.kotlinx.serialization.json)
  testImplementation(libs.junit)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.robolectric)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
