plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt.android)
}

import java.util.Properties
import java.io.FileInputStream

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { load(it) }
    }
}

val vMajor = versionProps.getProperty("versionMajor", "1").toInt()
val vMinor = versionProps.getProperty("versionMinor", "0").toInt()
val vPatch = versionProps.getProperty("versionPatch", "0").toInt()
val vCode = versionProps.getProperty("versionCode", "1").toInt()
val vName = "$vMajor.$vMinor.$vPatch"

android {
    namespace = "com.hrshd1eux.attendly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hrshd1eux.attendly"
        minSdk = 26
        targetSdk = 35
        versionCode = vCode
        versionName = vName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val releaseKeystorePath = project.findProperty("KEYSTORE_FILE") as? String
        ?: System.getenv("KEYSTORE_FILE")
    val releaseKeystorePassword = project.findProperty("KEYSTORE_PASSWORD") as? String
        ?: System.getenv("KEYSTORE_PASSWORD")
    val releaseKeyAlias = project.findProperty("KEY_ALIAS") as? String
        ?: System.getenv("KEY_ALIAS")
    val releaseKeyPassword = project.findProperty("KEY_PASSWORD") as? String
        ?: System.getenv("KEY_PASSWORD")

    signingConfigs {
        create("release") {
            if (releaseKeystorePath != null && file(releaseKeystorePath).exists()) {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null) {
                signingConfig = releaseConfig
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Glance Widget
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ZXing for QR Code Generation & Scanning
    implementation(libs.zxing.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

fun bumpVersion(type: String) {
    val propsFile = rootProject.file("version.properties")
    val props = Properties()
    if (propsFile.exists()) {
        propsFile.inputStream().use { props.load(it) }
    }
    var major = props.getProperty("versionMajor", "1").toInt()
    var minor = props.getProperty("versionMinor", "0").toInt()
    var patch = props.getProperty("versionPatch", "0").toInt()
    var code = props.getProperty("versionCode", "1").toInt()

    when (type.lowercase()) {
        "major" -> {
            major += 1
            minor = 0
            patch = 0
            code += 1
        }
        "minor" -> {
            minor += 1
            patch = 0
            code += 1
        }
        "patch" -> {
            patch += 1
            code += 1
        }
    }

    val content = """
        # Attendly Version Properties
        # Managed automatically by CI/CD workflow and local development
        versionMajor=$major
        versionMinor=$minor
        versionPatch=$patch
        versionCode=$code
    """.trimIndent() + "\n"

    propsFile.writeText(content)
    println("Version updated to v$major.$minor.$patch (versionCode: $code)")
}

tasks.register("bumpPatch") {
    group = "versioning"
    description = "Increments patch version and versionCode in version.properties"
    doLast { bumpVersion("patch") }
}

tasks.register("bumpMinor") {
    group = "versioning"
    description = "Increments minor version and versionCode in version.properties"
    doLast { bumpVersion("minor") }
}

tasks.register("bumpMajor") {
    group = "versioning"
    description = "Increments major version and versionCode in version.properties"
    doLast { bumpVersion("major") }
}

tasks.register("bumpVersion") {
    group = "versioning"
    description = "Alias for bumpPatch"
    dependsOn("bumpPatch")
}

