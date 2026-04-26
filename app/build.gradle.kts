import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ru.itmaster.schedule"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.itmaster.schedule"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "2.6.0"
        buildConfigField("String", "FIXED_API_ORIGIN", "\"http://axobeast.ru\"")
        buildConfigField("String", "WEB_SITE_ORIGIN", "\"http://axobeast.ru\"")
        buildConfigField("String", "GITHUB_RELEASES_LATEST_API", "\"https://api.github.com/repos/axobeasty/it-master-android/releases/latest\"")
    }

    buildTypes {
        debug {
            val local = Properties()
            val lf = rootProject.file("local.properties")
            if (lf.exists()) {
                lf.inputStream().use { local.load(it) }
            }
            val unsafeSsl = local.getProperty("unsafeSsl", "false").equals("true", ignoreCase = true)
            // Только debug: обход проверки сертификата (см. local.properties). В release всегда false.
            buildConfigField("boolean", "ALLOW_INSECURE_SSL", if (unsafeSsl) "true" else "false")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "ALLOW_INSECURE_SSL", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
