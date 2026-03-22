# Foundation + UI Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bootstrap the Magic App Android project with a Nix dev environment,
two-mode navigation (Glance/Detail), dark theme, and placeholder tiles for all
feature components — producing an installable APK.

**Architecture:** Single-activity Jetpack Compose app. `AppViewModel` owns
top-level mode state (Glance vs Detail) and mic mute state. `NavHost` routes
between Glance and Detail screens. All feature panels are placeholders in this
plan; subsequent plans replace them with real implementations.

**Tech Stack:** Kotlin 1.9.22, Jetpack Compose BOM 2024.02.00, Navigation Compose
2.7.7, AndroidX ViewModel, Gradle 8.6, AGP 8.2.2, Nix `androidenv`

---

## File Map

```text
shell.nix                                              (update)
.envrc                                                 (create)
settings.gradle.kts                                    (create)
build.gradle.kts                                       (create)
gradle/wrapper/gradle-wrapper.properties               (generated)
app/build.gradle.kts                                   (create)
app/src/main/AndroidManifest.xml                       (create)
app/src/main/kotlin/com/rippy/magicapp/
  MagicApplication.kt                                  (create)
  MainActivity.kt                                      (create)
  ui/
    theme/
      Color.kt                                         (create)
      Type.kt                                          (create)
      Theme.kt                                         (create)
    AppMode.kt                                         (create)
    AppViewModel.kt                                    (create)
    navigation/
      Feature.kt                                       (create)
      AppNavHost.kt                                    (create)
    common/
      ControlsOverlay.kt                               (create)
    glance/
      GlanceScreen.kt                                  (create)
      HeroCard.kt                                      (create)
      QuickTile.kt                                     (create)
    detail/
      DetailScreen.kt                                  (create)
      Sidebar.kt                                       (create)
      FeaturePlaceholder.kt                            (create)
app/src/test/kotlin/com/rippy/magicapp/
  ui/AppViewModelTest.kt                               (create)
app/src/androidTest/kotlin/com/rippy/magicapp/
  ui/glance/GlanceScreenTest.kt                        (create)
  ui/detail/DetailScreenTest.kt                        (create)
.github/workflows/build.yml                            (create)
docs/index.html                                        (create)
```

---

### Task 1: Update shell.nix for Android development

**Files:**

- Modify: `shell.nix`

Replace the current `android-tools` placeholder with a full Android SDK via
`androidenv`. The SDK must provide build tools 34.0.0 and platform 29.

- [ ] **Step 1: Replace shell.nix**

```nix
{ pkgs ? import <nixpkgs> {} }:

let
  androidComposition = pkgs.androidenv.composeAndroidPackages {
    buildToolsVersions = [ "34.0.0" ];
    platformVersions = [ "29" ];
    includeEmulator = false;
    includeSystemImages = false;
  };
  androidSdk = androidComposition.androidsdk;
in
pkgs.mkShell {
  buildInputs = with pkgs; [
    jdk17
    gradle
    androidSdk
    ktlint
    nodePackages.markdownlint-cli
  ];

  ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
  ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
  JAVA_HOME = "${pkgs.jdk17.home}";

  shellHook = ''
    export PATH="${androidSdk}/libexec/android-sdk/platform-tools:$PATH"
    echo "Magic App dev shell ready."
    echo "  ANDROID_SDK_ROOT: $ANDROID_SDK_ROOT"
    echo "  java: $(java -version 2>&1 | head -1)"
    echo "  markdownlint: $(markdownlint --version)"
  '';
}
```

- [ ] **Step 2: Create .envrc**

```bash
use nix
```

- [ ] **Step 3: Enter the shell and verify**

```bash
nix-shell
adb version
java -version
```

Expected: `adb` and `java 17` both respond without errors.

- [ ] **Step 4: Commit**

```bash
git add shell.nix .envrc
git commit -m "fix: update shell.nix with full Android SDK via androidenv"
```

---

### Task 2: Create Gradle build files

**Files:**

- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Generate: `gradle/wrapper/gradle-wrapper.properties` (via gradle command)

- [ ] **Step 1: Create settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "magic-app"
include(":app")
```

- [ ] **Step 2: Create root build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
```

- [ ] **Step 3: Generate Gradle wrapper (run inside nix-shell)**

```bash
nix-shell --run "gradle wrapper --gradle-version 8.6"
```

Expected: `gradle/wrapper/` directory created with
`gradle-wrapper.jar` and `gradle-wrapper.properties`.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle/
git commit -m "feat: add root Gradle build files and wrapper"
```

---

### Task 3: Create app/build.gradle.kts

**Files:**

- Create: `app/build.gradle.kts`

- [ ] **Step 1: Create app directory structure**

```bash
mkdir -p app/src/main/kotlin/com/rippy/magicapp
mkdir -p app/src/test/kotlin/com/rippy/magicapp
mkdir -p app/src/androidTest/kotlin/com/rippy/magicapp
mkdir -p app/src/main/res
```

- [ ] **Step 2: Create app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rippy.magicapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rippy.magicapp"
        minSdk = 29
        targetSdk = 29
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 3: Verify Gradle sync (inside nix-shell)**

```bash
nix-shell --run "./gradlew dependencies --configuration debugRuntimeClasspath" 2>&1 | tail -5
```

Expected: dependency tree output, no "BUILD FAILED".

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/src/
git commit -m "feat: add app module build configuration with Compose dependencies"
```

---

### Task 4: Create AndroidManifest.xml

**Files:**

- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create manifest**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".MagicApplication"
        android:allowBackup="true"
        android:label="Magic App"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.DayNight.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="landscape"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

Note: `screenOrientation="landscape"` — Magic Box 2.0 display is landscape.
Only `INTERNET` is declared here; subsequent plans add permissions as features
are implemented.

- [ ] **Step 2: Add AppCompat theme dependency to app/build.gradle.kts**

Add to the `dependencies` block:

```kotlin
implementation("androidx.appcompat:appcompat:1.6.1")
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/build.gradle.kts
git commit -m "feat: add AndroidManifest with landscape orientation"
```

---

### Task 5: Create dark theme

**Files:**

- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/theme/Color.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/theme/Type.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/theme/Theme.kt`

- [ ] **Step 1: Create Color.kt**

```kotlin
package com.rippy.magicapp.ui.theme

import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0D1117)
val Surface = Color(0xFF161B22)
val SurfaceVariant = Color(0xFF1C2128)
val OnSurface = Color(0xFFE6EDF3)
val OnSurfaceVariant = Color(0xFF8B949E)
val Primary = Color(0xFF4FC3F7)
val PrimaryContainer = Color(0xFF0D3450)
val Accent = Color(0xFF81C784)
val Warning = Color(0xFFFFB74D)
val Error = Color(0xFFF85149)
val MicActive = Color(0xFF4FC3F7)
val MicMuted = Color(0xFFF85149)
```

- [ ] **Step 2: Create Type.kt**

```kotlin
package com.rippy.magicapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Colors must NOT be hardcoded in Typography — they belong in MaterialTheme.
// Text color is inherited from the theme color scheme at composable call sites.
val Typography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
    ),
)
```

- [ ] **Step 3: Create Theme.kt**

```kotlin
package com.rippy.magicapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = OnSurface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    primary = Primary,
    primaryContainer = PrimaryContainer,
    error = Error,
)

@Composable
fun MagicAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/rippy/magicapp/ui/theme/
git commit -m "feat: add dark theme for in-car display"
```

---

### Task 6: AppMode + AppViewModel with unit tests (TDD)

**Files:**

- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/AppMode.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/AppViewModel.kt`
- Create: `app/src/test/kotlin/com/rippy/magicapp/ui/AppViewModelTest.kt`

- [ ] **Step 1: Create AppMode.kt**

```kotlin
package com.rippy.magicapp.ui

enum class AppMode { GLANCE, DETAIL }
```

- [ ] **Step 2: Write the failing tests**

```kotlin
// app/src/test/kotlin/com/rippy/magicapp/ui/AppViewModelTest.kt
package com.rippy.magicapp.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AppViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial mode is GLANCE`() {
        assertEquals(AppMode.GLANCE, viewModel.mode.value)
    }

    @Test
    fun `toggleMode switches GLANCE to DETAIL`() {
        viewModel.toggleMode()
        assertEquals(AppMode.DETAIL, viewModel.mode.value)
    }

    @Test
    fun `toggleMode switches DETAIL back to GLANCE`() {
        viewModel.toggleMode()
        viewModel.toggleMode()
        assertEquals(AppMode.GLANCE, viewModel.mode.value)
    }

    @Test
    fun `initial mic state is not muted`() {
        assertFalse(viewModel.isMicMuted.value)
    }

    @Test
    fun `toggleMic mutes the mic`() {
        viewModel.toggleMic()
        assertTrue(viewModel.isMicMuted.value)
    }

    @Test
    fun `toggleMic unmutes a muted mic`() {
        viewModel.toggleMic()
        viewModel.toggleMic()
        assertFalse(viewModel.isMicMuted.value)
    }
}
```

- [ ] **Step 3: Run tests — expect FAIL (class not found)**

```bash
nix-shell --run "./gradlew :app:test --tests '*.AppViewModelTest' -i" 2>&1 | tail -10
```

Expected: BUILD FAILED — `AppViewModel` does not exist yet.

- [ ] **Step 4: Implement AppViewModel**

```kotlin
// app/src/main/kotlin/com/rippy/magicapp/ui/AppViewModel.kt
package com.rippy.magicapp.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppViewModel : ViewModel() {

    private val _mode = MutableStateFlow(AppMode.GLANCE)
    val mode: StateFlow<AppMode> = _mode.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    fun toggleMode() {
        _mode.update { if (it == AppMode.GLANCE) AppMode.DETAIL else AppMode.GLANCE }
    }

    fun toggleMic() {
        _isMicMuted.update { !it }
    }
}
```

- [ ] **Step 5: Run tests — expect PASS**

```bash
nix-shell --run "./gradlew :app:test --tests '*.AppViewModelTest'" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`, 6 tests passed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/rippy/magicapp/ui/AppMode.kt \
        app/src/main/kotlin/com/rippy/magicapp/ui/AppViewModel.kt \
        app/src/test/kotlin/com/rippy/magicapp/ui/AppViewModelTest.kt
git commit -m "feat: add AppViewModel with mode and mic state (TDD)"
```

---

### Task 7: Feature enum + navigation

**Files:**

- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/navigation/Feature.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/navigation/AppNavHost.kt`

- [ ] **Step 1: Create Feature.kt**

```kotlin
package com.rippy.magicapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class Feature(val label: String, val icon: ImageVector) {
    GPS("GPS", Icons.Default.LocationOn),
    WIFI("WiFi", Icons.Default.Wifi),
    BLUETOOTH("Bluetooth", Icons.Default.Bluetooth),
    MICROPHONE("Mic", Icons.Default.Mic),
    SYSTEM_INFO("System", Icons.Default.Info),
    COMPASS("Compass", Icons.Default.Explore),
    CLOCK("Clock", Icons.Default.AccessTime),
    CAR_EVENTS("Events", Icons.Default.DirectionsCar),
    WEB("Web", Icons.Default.Language),
    YOUTUBE("YouTube", Icons.Default.PlayCircle),
    NEWS("News", Icons.Default.Article),
    VOICE("AI", Icons.Default.SmartToy),
    LLM_SETTINGS("LLM", Icons.Default.Settings),
    AIRPLAY("AirPlay", Icons.Default.CastConnected);

    companion object {
        /** Tiles shown in the bottom row of Glance mode. */
        val glanceTiles = listOf(NEWS, YOUTUBE, WEB, VOICE)
    }
}
```

- [ ] **Step 2: Create AppNavHost.kt**

```kotlin
package com.rippy.magicapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rippy.magicapp.ui.AppMode
import com.rippy.magicapp.ui.AppViewModel
import com.rippy.magicapp.ui.detail.DetailScreen
import com.rippy.magicapp.ui.glance.GlanceScreen

private const val ROUTE_GLANCE = "glance"
private const val ROUTE_DETAIL = "detail/{feature}"

@Composable
fun AppNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val mode by viewModel.mode.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()

    // Drive navigation from ViewModel mode state using LaunchedEffect to avoid
    // calling navigate() directly in composition, which causes recomposition loops.
    LaunchedEffect(mode) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (mode == AppMode.GLANCE && currentRoute != ROUTE_GLANCE) {
            navController.navigate(ROUTE_GLANCE) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_GLANCE) {
        composable(ROUTE_GLANCE) {
            GlanceScreen(
                isMicMuted = isMicMuted,
                onToggleMic = viewModel::toggleMic,
                onToggleMode = viewModel::toggleMode,
                onNavigateToFeature = { feature ->
                    viewModel.toggleMode()
                    navController.navigate("detail/${feature.name}") {
                        popUpTo(ROUTE_GLANCE) { inclusive = false }
                    }
                },
            )
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument("feature") { type = NavType.StringType }),
        ) { backStackEntry ->
            val featureName = backStackEntry.arguments?.getString("feature")
            val selectedFeature = featureName?.let {
                runCatching { Feature.valueOf(it) }.getOrNull()
            } ?: Feature.GPS
            DetailScreen(
                selectedFeature = selectedFeature,
                isMicMuted = isMicMuted,
                onToggleMic = viewModel::toggleMic,
                onToggleMode = viewModel::toggleMode,
                onSelectFeature = { feature ->
                    // popUpTo(ROUTE_GLANCE) keeps the glance entry but clears any
                    // previous detail entries, preventing back-stack accumulation.
                    navController.navigate("detail/${feature.name}") {
                        popUpTo(ROUTE_GLANCE) { inclusive = false }
                    }
                },
            )
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/rippy/magicapp/ui/navigation/
git commit -m "feat: add Feature enum and AppNavHost"
```

---

### Task 8: ControlsOverlay

**Files:**

- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/common/ControlsOverlay.kt`

The three controls (mic mute, mode toggle, settings) are always shown
in the top-right corner of the hero card / detail header.

- [ ] **Step 1: Create ControlsOverlay.kt**

```kotlin
package com.rippy.magicapp.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rippy.magicapp.ui.theme.MicActive
import com.rippy.magicapp.ui.theme.MicMuted

@Composable
fun ControlsOverlay(
    isMicMuted: Boolean,
    onToggleMic: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleMic, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isMicMuted) "Unmute microphone" else "Mute microphone",
                tint = if (isMicMuted) MicMuted else MicActive,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onToggleMode, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.SpaceDashboard,
                contentDescription = "Toggle display mode",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/rippy/magicapp/ui/common/
git commit -m "feat: add ControlsOverlay (mic mute, mode toggle, settings)"
```

---

### Task 9: GlanceScreen

**Files:**

- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/glance/QuickTile.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/glance/HeroCard.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/glance/GlanceScreen.kt`
- Create: `app/src/androidTest/kotlin/com/rippy/magicapp/ui/glance/GlanceScreenTest.kt`

Note: instrumented tests (`androidTest`) require a connected device or
emulator. Run with `./gradlew connectedAndroidTest`. Unit tests (`test`) do
not need a device.

- [ ] **Step 1: Write the instrumented UI test first**

```kotlin
// app/src/androidTest/kotlin/com/rippy/magicapp/ui/glance/GlanceScreenTest.kt
package com.rippy.magicapp.ui.glance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rippy.magicapp.ui.navigation.Feature
import com.rippy.magicapp.ui.theme.MagicAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlanceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun glanceScreen_heroCardIsDisplayed() {
        composeTestRule.setContent {
            MagicAppTheme {
                GlanceScreen(
                    isMicMuted = false,
                    onToggleMic = {},
                    onToggleMode = {},
                    onNavigateToFeature = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("hero_card").assertIsDisplayed()
    }

    @Test
    fun glanceScreen_allQuickTilesDisplayed() {
        composeTestRule.setContent {
            MagicAppTheme {
                GlanceScreen(
                    isMicMuted = false,
                    onToggleMic = {},
                    onToggleMode = {},
                    onNavigateToFeature = {},
                )
            }
        }
        Feature.glanceTiles.forEach { feature ->
            composeTestRule
                .onNodeWithTag("quick_tile_${feature.name}")
                .assertIsDisplayed()
        }
    }
}
```

- [ ] **Step 2: Create QuickTile.kt**

```kotlin
package com.rippy.magicapp.ui.glance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rippy.magicapp.ui.navigation.Feature

@Composable
fun QuickTile(
    feature: Feature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("quick_tile_${feature.name}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = feature.label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
```

- [ ] **Step 3: Create HeroCard.kt**

```kotlin
package com.rippy.magicapp.ui.glance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rippy.magicapp.ui.common.ControlsOverlay

@Composable
fun HeroCard(
    isMicMuted: Boolean,
    onToggleMic: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.testTag("hero_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Speed — placeholder until GPS feature is implemented
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "— mph",
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Text(
                            text = "GPS not yet active",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            // Controls pinned to top-right
            ControlsOverlay(
                isMicMuted = isMicMuted,
                onToggleMic = onToggleMic,
                onToggleMode = onToggleMode,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
```

- [ ] **Step 4: Create GlanceScreen.kt**

```kotlin
package com.rippy.magicapp.ui.glance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rippy.magicapp.ui.navigation.Feature

@Composable
fun GlanceScreen(
    isMicMuted: Boolean,
    onToggleMic: () -> Unit,
    onToggleMode: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Top row: hero card + small status tiles
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeroCard(
                isMicMuted = isMicMuted,
                onToggleMic = onToggleMic,
                onToggleMode = onToggleMode,
                onOpenSettings = { onNavigateToFeature(Feature.LLM_SETTINGS) },
                modifier = Modifier.weight(2f).fillMaxHeight(),
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickTile(
                    feature = Feature.WIFI,
                    onClick = { onNavigateToFeature(Feature.WIFI) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                QuickTile(
                    feature = Feature.BLUETOOTH,
                    onClick = { onNavigateToFeature(Feature.BLUETOOTH) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }

        // Bottom row: quick-launch tiles — explicit height prevents collapse
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Feature.glanceTiles.forEach { feature ->
                QuickTile(
                    feature = feature,
                    onClick = { onNavigateToFeature(feature) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
```

- [ ] **Step 5: Run the instrumented tests (requires connected device)**

```bash
nix-shell --run "./gradlew connectedAndroidTest --tests '*.GlanceScreenTest'"
```

Expected: 2 tests pass. If no device is connected, skip this step and
run it once the Magic Box is available via `adb`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/rippy/magicapp/ui/glance/ \
        app/src/androidTest/kotlin/com/rippy/magicapp/ui/glance/
git commit -m "feat: add GlanceScreen with hero card and quick tiles"
```

---

### Task 10: DetailScreen

**Files:**

- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/detail/FeaturePlaceholder.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/detail/Sidebar.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/ui/detail/DetailScreen.kt`
- Create: `app/src/androidTest/kotlin/com/rippy/magicapp/ui/detail/DetailScreenTest.kt`

- [ ] **Step 1: Write the instrumented UI test first**

```kotlin
// app/src/androidTest/kotlin/com/rippy/magicapp/ui/detail/DetailScreenTest.kt
package com.rippy.magicapp.ui.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rippy.magicapp.ui.navigation.Feature
import com.rippy.magicapp.ui.theme.MagicAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun detailScreen_sidebarIsDisplayed() {
        composeTestRule.setContent {
            MagicAppTheme {
                DetailScreen(
                    selectedFeature = Feature.GPS,
                    isMicMuted = false,
                    onToggleMic = {},
                    onToggleMode = {},
                    onSelectFeature = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("detail_sidebar").assertIsDisplayed()
    }

    @Test
    fun detailScreen_featurePanelIsDisplayed() {
        composeTestRule.setContent {
            MagicAppTheme {
                DetailScreen(
                    selectedFeature = Feature.GPS,
                    isMicMuted = false,
                    onToggleMic = {},
                    onToggleMode = {},
                    onSelectFeature = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("feature_panel_${Feature.GPS.name}").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Create FeaturePlaceholder.kt**

```kotlin
package com.rippy.magicapp.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rippy.magicapp.ui.navigation.Feature

@Composable
fun FeaturePlaceholder(feature: Feature, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("feature_panel_${feature.name}"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = feature.label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
```

- [ ] **Step 3: Create Sidebar.kt**

```kotlin
package com.rippy.magicapp.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rippy.magicapp.ui.navigation.Feature

@Composable
fun Sidebar(
    selectedFeature: Feature,
    onSelectFeature: (Feature) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .testTag("detail_sidebar"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Feature.entries.forEach { feature ->
            val isSelected = feature == selectedFeature
            // IconButton provides a 48dp touch target and ripple automatically.
            IconButton(onClick = { onSelectFeature(feature) }) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.label,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 4: Create DetailScreen.kt**

```kotlin
package com.rippy.magicapp.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rippy.magicapp.ui.common.ControlsOverlay
import com.rippy.magicapp.ui.navigation.Feature

@Composable
fun DetailScreen(
    selectedFeature: Feature,
    isMicMuted: Boolean,
    onToggleMic: () -> Unit,
    onToggleMode: () -> Unit,
    onSelectFeature: (Feature) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Sidebar(
            selectedFeature = selectedFeature,
            onSelectFeature = onSelectFeature,
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(8.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            // Replace FeaturePlaceholder with real composable in later plans.
            Box {
                FeaturePlaceholder(feature = selectedFeature)
                ControlsOverlay(
                    isMicMuted = isMicMuted,
                    onToggleMic = onToggleMic,
                    onToggleMode = onToggleMode,
                    onOpenSettings = { onSelectFeature(Feature.LLM_SETTINGS) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 5: Run instrumented tests (requires connected device)**

```bash
nix-shell --run "./gradlew connectedAndroidTest --tests '*.DetailScreenTest'"
```

Expected: 2 tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/rippy/magicapp/ui/detail/ \
        app/src/androidTest/kotlin/com/rippy/magicapp/ui/detail/
git commit -m "feat: add DetailScreen with sidebar and feature placeholder panel"
```

---

### Task 11: MainActivity, MagicApplication, wire everything

**Files:**

- Create: `app/src/main/kotlin/com/rippy/magicapp/MagicApplication.kt`
- Create: `app/src/main/kotlin/com/rippy/magicapp/MainActivity.kt`

- [ ] **Step 1: Create MagicApplication.kt**

```kotlin
package com.rippy.magicapp

import android.app.Application

class MagicApplication : Application()
```

- [ ] **Step 2: Create MainActivity.kt**

```kotlin
package com.rippy.magicapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.rippy.magicapp.ui.AppViewModel
import com.rippy.magicapp.ui.navigation.AppNavHost
import com.rippy.magicapp.ui.theme.MagicAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MagicAppTheme {
                AppNavHost(viewModel = viewModel)
            }
        }
    }
}
```

- [ ] **Step 3: Build the APK**

```bash
nix-shell --run "./gradlew :app:assembleDebug" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`. APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Install on device (Magic Box connected via adb)**

```bash
nix-shell --run "adb install -r app/build/outputs/apk/debug/app-debug.apk"
```

Expected: `Performing Streamed Install`, `Success`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/rippy/magicapp/MagicApplication.kt \
        app/src/main/kotlin/com/rippy/magicapp/MainActivity.kt
git commit -m "feat: add MainActivity and wire up full app shell"
```

---

### Task 12: GitHub Actions CI workflow

**Files:**

- Create: `.github/workflows/build.yml`

- [ ] **Step 1: Create workflow file**

```bash
mkdir -p .github/workflows
```

```yaml
# .github/workflows/build.yml
name: Build

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3
        with:
          accept-android-sdk-licenses: true
          packages: |
            platforms;android-29
            build-tools;34.0.0
            platform-tools

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run unit tests
        run: ./gradlew :app:test

      - name: Build debug APK
        run: ./gradlew :app:assembleDebug

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: magic-app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
          retention-days: 7
```

- [ ] **Step 2: Commit and push**

```bash
git add .github/
git commit -m "ci: add GitHub Actions build workflow"
git push origin main
```

- [ ] **Step 3: Verify CI passes**

Open the repo on GitHub → Actions tab. Confirm the workflow run succeeds.

---

### Task 13: GitHub Pages sideload landing page

**Files:**

- Create: `docs/index.html`

This replaces the existing spec-only `docs/` directory with a proper GitHub
Pages root. The page always links to the latest GitHub Release APK.

- [ ] **Step 1: Create docs/index.html**

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Magic App</title>
  <style>
    body {
      background: #0d1117;
      color: #e6edf3;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      margin: 0;
    }
    h1 { font-size: 2rem; margin-bottom: 0.25rem; }
    p { color: #8b949e; margin-bottom: 2rem; }
    a.btn {
      background: #4fc3f7;
      color: #0d1117;
      text-decoration: none;
      font-weight: bold;
      font-size: 1.1rem;
      padding: 0.75rem 2rem;
      border-radius: 6px;
    }
    a.btn:hover { background: #81d4fa; }
  </style>
</head>
<body>
  <h1>Magic App</h1>
  <p>In-car companion for Magic Box 2.0</p>
  <!-- Direct APK asset link is set up in Plan 5 (Distribution).
       Until then, link to the releases page so users can download manually. -->
  <a class="btn"
     href="https://github.com/rippy/magic-app/releases/latest">
    Latest Release
  </a>
</body>
</html>
```

- [ ] **Step 2: Enable GitHub Pages in repo settings**

Go to repo → Settings → Pages → Source: `main` branch, `/docs` folder.

- [ ] **Step 3: Commit**

```bash
git add docs/index.html
git commit -m "feat: add GitHub Pages sideload landing page"
git push origin main
```

- [ ] **Step 4: Verify the page loads**

Visit `https://rippy.github.io/magic-app/`. Confirm the download button is
visible.

---

## Running all unit tests

At any point, run all unit tests (no device required):

```bash
nix-shell --run "./gradlew :app:test"
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

## Running all markdown lint checks

```bash
nix-shell --run "markdownlint '**/*.md' --ignore node_modules"
```

Expected: no output, exit 0.
