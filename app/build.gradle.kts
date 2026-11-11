import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.vesper.audio"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.sonora.musicaudio"
    minSdk = 24
    targetSdk = 36
    versionCode = 31
    versionName = "3.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }


  // Production signing is configured only from maintainer-supplied environment variables.
  // Never store the keystore or passwords in the repository.
  val releaseKeystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
  val releaseStorePassword = providers.environmentVariable("STORE_PASSWORD").orNull
  val releaseKeyAlias = providers.environmentVariable("KEY_ALIAS").orNull
  val releaseKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
  val releaseKeystoreFile = releaseKeystorePath?.let { rootProject.file(it) }
  val releaseSigningConfigured = releaseKeystoreFile?.isFile == true &&
      !releaseStorePassword.isNullOrBlank() &&
      !releaseKeyAlias.isNullOrBlank() &&
      !releaseKeyPassword.isNullOrBlank()

  signingConfigs {
    if (releaseSigningConfigured) {
      create("release") {
        storeFile = releaseKeystoreFile
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (releaseSigningConfigured) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    // Use the standard Android debug signing configuration; no custom debug key is required.
    debug {
      isDebuggable = true
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


// Prevent accidental distribution of an unsigned or debug-key-signed release artifact.
val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
  doLast {
    val configuredPath = providers.environmentVariable("KEYSTORE_PATH").orNull
    val configuredStorePassword = providers.environmentVariable("STORE_PASSWORD").orNull
    val configuredAlias = providers.environmentVariable("KEY_ALIAS").orNull
    val configuredKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
    val keyFile = configuredPath?.let { rootProject.file(it) }
    check(keyFile?.isFile == true &&
        !configuredStorePassword.isNullOrBlank() &&
        !configuredAlias.isNullOrBlank() &&
        !configuredKeyPassword.isNullOrBlank()) {
      "Release signing is not configured. Set KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD in the build environment."
    }
  }
}
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
  dependsOn(verifyReleaseSigning)
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.common)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.withType<Test>().configureEach {
  systemProperty("robolectric.dependency.dir", "/home/appuser/.m2/repository/org/robolectric/android-all-instrumented/16-robolectric-13921718-i7")
}
