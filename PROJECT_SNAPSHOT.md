# FastFileSearch - لقطة هندسية شاملة للمشروع (Project Snapshot)

> **تاريخ التصدير:** 2026-11-22 22:36:16  
> **إجمالي الملفات المضمّنة:** 30 ملفًا

## ملخص الحالة

### ناتج `git status --short`:

```text
M app/src/main/java/com/example/fastfilesearch/MainActivity.kt
 M app/src/main/java/com/example/fastfilesearch/SearchEngine.kt
 M app/src/main/java/com/example/fastfilesearch/SearchViewModel.kt
 M app/src/main/java/com/example/fastfilesearch/ui/SearchScreen.kt
```

### آخر 5 تعديلات من `git log -n 5 --oneline`:

```text
edf0060 feat: add SearchScreen UI with support for grouped results and Excel grid view
036739e feat: add SearchScreen UI component for fast file searching and results display
bba83fc feat: add SearchScreen UI and TableCellHelper for displaying search results
b85cab6 feat: initial project structure and build configuration for Android app
d997165 first commit
```

## شجرة ملفات الكود

قائمة بجميع ملفات المصدر والإعدادات المضمّنة (30 ملفًا):

1. `settings.gradle.kts`
2. `build.gradle.kts`
3. `app/build.gradle.kts`
4. `gradle/libs.versions.toml`
5. `gradle.properties`
6. `app/src/main/AndroidManifest.xml`
7. `app/src/main/keepRules/rules.keep`
8. `app/src/main/res/values/strings.xml`
9. `app/src/androidTest/java/com/example/fastfilesearch/ExampleInstrumentedTest.kt`
10. `app/src/main/java/com/example/fastfilesearch/MainActivity.kt`
11. `app/src/main/java/com/example/fastfilesearch/SearchEngine.kt`
12. `app/src/main/java/com/example/fastfilesearch/SearchViewModel.kt`
13. `app/src/main/java/com/example/fastfilesearch/database/SearchDatabaseHelper.kt`
14. `app/src/main/java/com/example/fastfilesearch/model/FileModels.kt`
15. `app/src/main/java/com/example/fastfilesearch/parser/DocxParser.kt`
16. `app/src/main/java/com/example/fastfilesearch/parser/ExcelXlsParser.kt`
17. `app/src/main/java/com/example/fastfilesearch/parser/ExcelXlsxParser.kt`
18. `app/src/main/java/com/example/fastfilesearch/parser/FileParserDispatcher.kt`
19. `app/src/main/java/com/example/fastfilesearch/parser/PdfParser.kt`
20. `app/src/main/java/com/example/fastfilesearch/parser/TextFileParser.kt`
21. `app/src/main/java/com/example/fastfilesearch/ui/FileTypeHelper.kt`
22. `app/src/main/java/com/example/fastfilesearch/ui/SearchScreen.kt`
23. `app/src/main/java/com/example/fastfilesearch/ui/TableCellHelper.kt`
24. `app/src/main/java/com/example/fastfilesearch/ui/TextHighlighter.kt`
25. `app/src/main/java/com/example/fastfilesearch/ui/theme/Color.kt`
26. `app/src/main/java/com/example/fastfilesearch/ui/theme/Theme.kt`
27. `app/src/main/java/com/example/fastfilesearch/ui/theme/Type.kt`
28. `app/src/main/java/com/example/fastfilesearch/util/ArabicNormalizer.kt`
29. `app/src/test/java/com/example/fastfilesearch/ArabicNormalizerTest.kt`
30. `app/src/test/java/com/example/fastfilesearch/ExampleUnitTest.kt`

---

## محتويات الملفات

## settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FastFileSearch"
include(":app")
 
```

## build.gradle.kts

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

## app/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.fastfilesearch"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.fastfilesearch"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.documentfile)

    // Document & Spreadsheet parsing libraries
    implementation(libs.jxl)
    implementation(libs.pdfbox.android)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

## gradle/libs.versions.toml

```toml
[versions]
agp = "9.3.3"
coreKtx = "1.19.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.11.0"
activityCompose = "1.13.0"
kotlin = "2.2.10"
composeBom = "2026.02.01"
fastexcel = "0.18.4"
jxl = "2.6.12"
pdfbox = "2.0.27.0"
documentfile = "1.0.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-documentfile = { group = "androidx.documentfile", name = "documentfile", version.ref = "documentfile" }
fastexcel-reader = { group = "org.dhatim", name = "fastexcel-reader", version.ref = "fastexcel" }
jxl = { group = "net.sourceforge.jexcelapi", name = "jxl", version.ref = "jxl" }
pdfbox-android = { group = "com.tom-roush", name = "pdfbox-android", version.ref = "pdfbox" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

## gradle.properties

```properties
# Project-wide Gradle settings.
# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.
# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html
# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000
# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. For more details, visit
# https://developer.android.com/r/tools/gradle-multi-project-decoupled-projects
# org.gradle.parallel=true
# When enabled, the Configuration Cache allows Gradle to skip the configuration
# phase entirely if nothing that affects the build configuration (such as build scripts)
# has changed. Additionally, Gradle applies performance optimizations to task execution.
org.gradle.configuration-cache=false
# Kotlin code style for this project: "official" or "obsolete":
kotlin.code.style=official
```

## app/src/main/AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FastFileSearch">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.FastFileSearch"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

## app/src/main/keepRules/rules.keep

```properties
# Add project specific R8 rules here.
# AGP will combine all keep rule files in src/main/keepRules to pass to R8
#
# For more details, see
#   https://d.android.com/r/tools/r8/keep-rules

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
```

## app/src/main/res/values/strings.xml

```xml
<resources>
    <string name="app_name">البحث السريع في الملفات</string>
</resources>
```

## app/src/androidTest/java/com/example/fastfilesearch/ExampleInstrumentedTest.kt

```kotlin
package com.example.fastfilesearch

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.fastfilesearch", appContext.packageName)
    }
}
```

## app/src/main/java/com/example/fastfilesearch/MainActivity.kt

```kotlin
package com.example.fastfilesearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.documentfile.provider.DocumentFile
import com.example.fastfilesearch.ui.SearchScreen
import com.example.fastfilesearch.ui.theme.FastFileSearchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FastFileSearchTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Folder Picker Contract using SAF
                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        val docFile = DocumentFile.fromTreeUri(this, uri)
                        val folderName = docFile?.name ?: uri.lastPathSegment ?: "المجلد المحدد"
                        viewModel.onFolderSelected(uri, folderName)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SearchScreen(
                        uiState = uiState,
                        onSelectFolderClick = { folderPickerLauncher.launch(null) },
                        onRefreshClick = { viewModel.onRefreshClicked() },
                        onQueryChanged = { viewModel.onQueryChanged(it) },
                        onClearQuery = { viewModel.onClearQuery() },
                        onOpenFile = { viewModel.onOpenFile(it) },
                        onDismissMessage = { viewModel.onDismissMessage() },
                        onToggleStretchSingleColumn = { viewModel.onToggleStretchSingleColumn(it) }
                    )
                }
            }
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/SearchEngine.kt

```kotlin
package com.example.fastfilesearch

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.fastfilesearch.database.SearchDatabaseHelper
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.model.IndexedFileRecord
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.parser.FileParserDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

data class IndexProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentFileName: String = "",
    val isIndexing: Boolean = false,
    val statusMessage: String = ""
)

data class SyncSummary(
    val totalFound: Int,
    val indexedCount: Int,
    val skippedCount: Int,
    val deletedCount: Int
)

data class DiscoveredFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?
)

class SearchEngine(private val context: Context) {

    companion object {
        private const val TAG = "SearchEngine"
        private const val PREFS_NAME = "fast_file_search_prefs"
        private const val KEY_FOLDER_URI = "saved_folder_uri"
        private const val KEY_FOLDER_NAME = "saved_folder_name"
        private const val KEY_STRETCH_SINGLE_COLUMN = "stretch_single_column"
        private const val PARALLEL_WORKERS = 4
    }

    private val dbHelper = SearchDatabaseHelper.getInstance(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedFolderUri(): Uri? {
        val uriStr = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (e: Exception) {
            null
        }
    }

    fun getSavedFolderName(): String {
        return prefs.getString(KEY_FOLDER_NAME, "") ?: ""
    }

    fun saveFolderUri(uri: Uri, folderName: String = "") {
        // Persist SAF permissions
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to take persistable URI permission", e)
        }

        val resolvedName = if (folderName.isNotBlank()) {
            folderName
        } else {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile?.name ?: uri.lastPathSegment ?: "مجلد محدد"
        }

        prefs.edit()
            .putString(KEY_FOLDER_URI, uri.toString())
            .putString(KEY_FOLDER_NAME, resolvedName)
            .apply()
    }

    fun clearSavedFolder() {
        prefs.edit().remove(KEY_FOLDER_URI).remove(KEY_FOLDER_NAME).apply()
        dbHelper.clearAll()
    }

    fun isStretchSingleColumn(): Boolean {
        return prefs.getBoolean(KEY_STRETCH_SINGLE_COLUMN, true)
    }

    fun setStretchSingleColumn(stretch: Boolean) {
        prefs.edit().putBoolean(KEY_STRETCH_SINGLE_COLUMN, stretch).apply()
    }

    /**
     * Executes Delta Sync:
     * 1. Scans folder tree.
     * 2. Compares signature (uri, size, lastModified) against DB.
     * 3. Deletes records of removed files.
     * 4. Indexes new or modified files in parallel (4 files concurrently).
     * 5. Skips unchanged files (0 I/O).
     */
    suspend fun syncFolder(
        treeUri: Uri,
        onProgress: (IndexProgress) -> Unit = {}
    ): SyncSummary = withContext(Dispatchers.IO) {
        onProgress(IndexProgress(isIndexing = true, statusMessage = "جاري مسح المجلد..."))

        // 1. Discover all candidate files
        val discoveredFiles = scanDirectoryTree(treeUri)
        val discoveredMap = discoveredFiles.associateBy { it.uri.toString() }

        // 2. Fetch existing indexed files footprint from SQLite
        val indexedMap = dbHelper.getAllIndexedFiles()

        // 3. Remove deleted files from DB
        var deletedCount = 0
        for ((existingUri, _) in indexedMap) {
            if (!discoveredMap.containsKey(existingUri)) {
                dbHelper.deleteFileIndex(existingUri)
                deletedCount++
            }
        }

        // 4. Identify new and modified files
        val filesToIndex = mutableListOf<DiscoveredFile>()
        var skippedCount = 0

        for (file in discoveredFiles) {
            val existing = indexedMap[file.uri.toString()]
            if (existing != null && existing.size == file.size && existing.lastModified == file.lastModified) {
                skippedCount++
            } else {
                filesToIndex.add(file)
            }
        }

        val totalToIndex = filesToIndex.size
        val processedCounter = AtomicInteger(0)

        if (totalToIndex > 0) {
            onProgress(
                IndexProgress(
                    current = 0,
                    total = totalToIndex,
                    currentFileName = "",
                    isIndexing = true,
                    statusMessage = "فهرسة $totalToIndex ملف..."
                )
            )

            val semaphore = Semaphore(PARALLEL_WORKERS)
            coroutineScope {
                filesToIndex.map { file ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val count = processedCounter.incrementAndGet()
                            onProgress(
                                IndexProgress(
                                    current = count,
                                    total = totalToIndex,
                                    currentFileName = file.name,
                                    isIndexing = true,
                                    statusMessage = "فهرسة (${count}/$totalToIndex): ${file.name}"
                                )
                            )

                            indexSingleFile(file)
                        }
                    }
                }.awaitAll()
            }
        }

        onProgress(
            IndexProgress(
                current = totalToIndex,
                total = totalToIndex,
                currentFileName = "",
                isIndexing = false,
                statusMessage = "اكتملت المزامنة"
            )
        )

        SyncSummary(
            totalFound = discoveredFiles.size,
            indexedCount = totalToIndex,
            skippedCount = skippedCount,
            deletedCount = deletedCount
        )
    }

    private fun indexSingleFile(file: DiscoveredFile) {
        try {
            val segments = mutableListOf<FileSegment>()
            context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                FileParserDispatcher.parseFile(
                    context = context,
                    inputStream = inputStream,
                    fileUri = file.uri.toString(),
                    fileName = file.name
                ) { segment ->
                    segments.add(segment)
                }
            }

            val record = IndexedFileRecord(
                uri = file.uri.toString(),
                fileName = file.name,
                mimeType = file.mimeType,
                size = file.size,
                lastModified = file.lastModified
            )

            dbHelper.saveFileAndSegments(record, segments)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed indexing file ${file.name} at ${file.uri}", t)
        }
    }

    private fun scanDirectoryTree(treeUri: Uri): List<DiscoveredFile> {
        val results = mutableListOf<DiscoveredFile>()

        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            scanFolderRecursive(treeUri, rootDocId, results)
        } catch (e: Exception) {
            Log.w(TAG, "Direct DocumentsContract tree scan failed, falling back to DocumentFile", e)
            val docFile = DocumentFile.fromTreeUri(context, treeUri)
            if (docFile != null && docFile.isDirectory) {
                scanDocumentFileRecursive(docFile, results)
            }
        }

        return results
    }

    private fun scanFolderRecursive(
        treeUri: Uri,
        folderDocId: String,
        results: MutableList<DiscoveredFile>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        try {
            val cursor = context.contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (it.moveToNext()) {
                    val docId = it.getString(idIdx)
                    val name = it.getString(nameIdx) ?: continue
                    val mime = it.getString(mimeIdx)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanFolderRecursive(treeUri, docId, results)
                    } else {
                        // Ignore temporary Office files (~$) and unsupported extensions
                        if (FileParserDispatcher.isSupportedFile(name)) {
                            val size = if (it.isNull(sizeIdx)) 0L else it.getLong(sizeIdx)
                            val modified = if (it.isNull(modIdx)) 0L else it.getLong(modIdx)
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            results.add(
                                DiscoveredFile(
                                    uri = fileUri,
                                    name = name,
                                    size = size,
                                    lastModified = modified,
                                    mimeType = mime
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder with docId: $folderDocId", e)
        }
    }

    private fun scanDocumentFileRecursive(
        directory: DocumentFile,
        results: MutableList<DiscoveredFile>
    ) {
        val files = directory.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                scanDocumentFileRecursive(file, results)
            } else {
                val name = file.name ?: continue
                if (FileParserDispatcher.isSupportedFile(name)) {
                    results.add(
                        DiscoveredFile(
                            uri = file.uri,
                            name = name,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            mimeType = file.type
                        )
                    )
                }
            }
        }
    }

    /**
     * Executes instant FTS5 search.
     */
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        dbHelper.search(query)
    }

    /**
     * Opens the file in its default external application.
     */
    fun openFile(fileUriString: String): Result<Unit> {
        return try {
            val uri = Uri.parse(fileUriString)
            val mimeType = context.contentResolver.getType(uri) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            // Fallback to generic */* type
            try {
                val uri = Uri.parse(fileUriString)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to open file: $fileUriString", fallbackEx)
                Result.failure(fallbackEx)
            }
        }
    }

    fun getIndexStats(): Pair<Int, Int> {
        return dbHelper.getStats()
    }
}
```

## app/src/main/java/com/example/fastfilesearch/SearchViewModel.kt

```kotlin
package com.example.fastfilesearch

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fastfilesearch.model.SearchResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val folderUri: Uri? = null,
    val folderName: String = "",
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val indexProgress: IndexProgress = IndexProgress(),
    val filesCount: Int = 0,
    val segmentsCount: Int = 0,
    val userMessage: String? = null,
    val stretchSingleColumn: Boolean = true
)

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchEngine = SearchEngine(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Restore saved folder & preferences
        val savedUri = searchEngine.getSavedFolderUri()
        val savedName = searchEngine.getSavedFolderName()
        val stats = searchEngine.getIndexStats()
        val stretchPref = searchEngine.isStretchSingleColumn()

        _uiState.update {
            it.copy(
                folderUri = savedUri,
                folderName = savedName,
                filesCount = stats.first,
                segmentsCount = stats.second,
                stretchSingleColumn = stretchPref
            )
        }

        // Setup debounced search query listener
        viewModelScope.launch {
            searchQueryFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { query ->
                    executeSearch(query)
                }
        }

        // Auto delta-sync on startup if a folder is already configured
        if (savedUri != null) {
            syncFolder(savedUri)
        }
    }

    fun onFolderSelected(uri: Uri, displayName: String = "") {
        searchEngine.saveFolderUri(uri, displayName)
        val name = searchEngine.getSavedFolderName()
        _uiState.update {
            it.copy(
                folderUri = uri,
                folderName = name,
                userMessage = "تم اختيار المجلد بنجاح"
            )
        }
        syncFolder(uri)
    }

    fun onRefreshClicked() {
        val uri = _uiState.value.folderUri ?: return
        syncFolder(uri)
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        searchQueryFlow.value = newQuery
    }

    fun onClearQuery() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false
            )
        }
        searchQueryFlow.value = ""
    }

    private fun executeSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false
                )
            }
            return
        }

        _uiState.update { it.copy(isSearching = true) }

        viewModelScope.launch {
            try {
                val results = searchEngine.search(query)
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        userMessage = "حدث خطأ أثناء البحث: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun syncFolder(uri: Uri) {
        if (_uiState.value.indexProgress.isIndexing) return

        viewModelScope.launch {
            try {
                val summary = searchEngine.syncFolder(uri) { progress ->
                    _uiState.update { it.copy(indexProgress = progress) }
                }

                val stats = searchEngine.getIndexStats()
                val message = when {
                    summary.indexedCount > 0 ->
                        "اكتملت الفهرسة: تم تحديث ${summary.indexedCount} ملف، وتخطي ${summary.skippedCount} ملف دون تغيير."
                    summary.deletedCount > 0 ->
                        "تم حذف ${summary.deletedCount} ملف غير موجود."
                    else ->
                        "جميع الملفات محدثة (${summary.skippedCount} ملف)."
                }

                _uiState.update {
                    it.copy(
                        filesCount = stats.first,
                        segmentsCount = stats.second,
                        userMessage = message
                    )
                }

                // If user currently has an active query, refresh search results with updated index
                val currentQuery = _uiState.value.searchQuery
                if (currentQuery.isNotBlank()) {
                    executeSearch(currentQuery)
                }
            } catch (t: Throwable) {
                android.util.Log.e("SearchViewModel", "Folder sync error", t)
                _uiState.update {
                    it.copy(
                        indexProgress = IndexProgress(isIndexing = false),
                        userMessage = "فشل مزامنة المجلد: ${t.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onOpenFile(fileUri: String) {
        val result = searchEngine.openFile(fileUri)
        if (result.isFailure) {
            _uiState.update {
                it.copy(userMessage = "تعذر فتح الملف، تأكد من توفر تطبيق مناسب على الهاتف")
            }
        }
    }

    fun onToggleStretchSingleColumn(stretch: Boolean) {
        searchEngine.setStretchSingleColumn(stretch)
        _uiState.update { it.copy(stretchSingleColumn = stretch) }
    }

    fun onDismissMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/database/SearchDatabaseHelper.kt

```kotlin
package com.example.fastfilesearch.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.util.Log
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.model.IndexedFileRecord
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.util.ArabicNormalizer

class SearchDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val TAG = "SearchDatabaseHelper"
        private const val DATABASE_NAME = "fast_file_search.db"
        // Upgraded to version 2 for universal FTS compatibility across all Android OEMs
        private const val DATABASE_VERSION = 2

        private const val TABLE_FILES = "indexed_files"
        private const val COL_FILE_URI = "file_uri"
        private const val COL_FILE_NAME = "file_name"
        private const val COL_MIME_TYPE = "mime_type"
        private const val COL_FILE_SIZE = "file_size"
        private const val COL_LAST_MODIFIED = "last_modified"
        private const val COL_INDEXED_AT = "indexed_at"

        private const val TABLE_FTS = "file_segments_fts"
        private const val FTS_COL_URI = "file_uri"
        private const val FTS_COL_NAME = "file_name"
        private const val FTS_COL_LOCATION = "location"
        private const val FTS_COL_CONTENT = "content"
        private const val FTS_COL_NORMALIZED = "normalized_content"

        @Volatile
        private var instance: SearchDatabaseHelper? = null

        fun getInstance(context: Context): SearchDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: SearchDatabaseHelper(context).also { instance = it }
            }
        }
    }

    @Volatile
    private var isUsingFts5: Boolean? = null

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "Upgrading database from $oldVersion to $newVersion")
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FILES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FTS")
        } catch (e: Exception) {
            Log.e(TAG, "Error dropping tables during upgrade", e)
        }
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        // 1. Files metadata table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FILES (
                $COL_FILE_URI TEXT PRIMARY KEY,
                $COL_FILE_NAME TEXT NOT NULL,
                $COL_MIME_TYPE TEXT,
                $COL_FILE_SIZE INTEGER NOT NULL,
                $COL_LAST_MODIFIED INTEGER NOT NULL,
                $COL_INDEXED_AT INTEGER NOT NULL
            );
            """.trimIndent()
        )

        // 2. Determine FTS module availability
        // Some Android OEM builds (e.g., Samsung on Android 10) omit FTS5 compile flag.
        // FTS4 is universally supported on 100% of Android devices.
        val fts5Available = probeFts5(db)

        if (fts5Available) {
            Log.i(TAG, "FTS5 is supported. Creating FTS5 virtual table.")
            try {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts5(
                        $FTS_COL_URI UNINDEXED,
                        $FTS_COL_NAME,
                        $FTS_COL_LOCATION UNINDEXED,
                        $FTS_COL_CONTENT UNINDEXED,
                        $FTS_COL_NORMALIZED,
                        tokenize = 'unicode61'
                    );
                    """.trimIndent()
                )
                isUsingFts5 = true
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed creating FTS5 table despite probe, falling back to FTS4", e)
            }
        }

        // Fallback to FTS4
        Log.i(TAG, "Using FTS4 virtual table for maximum device compatibility.")
        val fts4Unicode61 = probeFts4Unicode61(db)
        val tokenizeClause = if (fts4Unicode61) "tokenize=unicode61" else ""

        val fts4Sql = buildString {
            append("CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts4(")
            append("$FTS_COL_URI, ")
            append("$FTS_COL_NAME, ")
            append("$FTS_COL_LOCATION, ")
            append("$FTS_COL_CONTENT, ")
            append("$FTS_COL_NORMALIZED, ")
            append("notindexed=$FTS_COL_URI, ")
            append("notindexed=$FTS_COL_LOCATION, ")
            append("notindexed=$FTS_COL_CONTENT")
            if (tokenizeClause.isNotEmpty()) {
                append(", $tokenizeClause")
            }
            append(");")
        }

        db.execSQL(fts4Sql)
        isUsingFts5 = false
    }

    private fun probeFts5(db: SQLiteDatabase): Boolean {
        return try {
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS _probe_fts5 USING fts5(x);")
            db.execSQL("DROP TABLE IF EXISTS _probe_fts5;")
            true
        } catch (e: Exception) {
            Log.d(TAG, "FTS5 probe failed: ${e.message}")
            false
        }
    }

    private fun probeFts4Unicode61(db: SQLiteDatabase): Boolean {
        return try {
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS _probe_fts4 USING fts4(x, tokenize=unicode61);")
            db.execSQL("DROP TABLE IF EXISTS _probe_fts4;")
            true
        } catch (e: Exception) {
            Log.d(TAG, "FTS4 unicode61 probe failed: ${e.message}")
            false
        }
    }

    private fun checkIsFts5(db: SQLiteDatabase): Boolean {
        isUsingFts5?.let { return it }

        var usingFts5 = false
        try {
            val cursor = db.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='$TABLE_FTS'",
                null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val sql = it.getString(0) ?: ""
                    usingFts5 = sql.contains("fts5", ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine FTS type from sqlite_master", e)
        }

        isUsingFts5 = usingFts5
        return usingFts5
    }

    /**
     * Retrieves all indexed files to compare file footprint (uri, size, lastModified) for Delta Sync.
     */
    fun getAllIndexedFiles(): Map<String, IndexedFileRecord> {
        val result = mutableMapOf<String, IndexedFileRecord>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COL_FILE_URI, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT FROM $TABLE_FILES",
                null
            )
            cursor.use {
                val uriIdx = it.getColumnIndexOrThrow(COL_FILE_URI)
                val nameIdx = it.getColumnIndexOrThrow(COL_FILE_NAME)
                val mimeIdx = it.getColumnIndexOrThrow(COL_MIME_TYPE)
                val sizeIdx = it.getColumnIndexOrThrow(COL_FILE_SIZE)
                val modIdx = it.getColumnIndexOrThrow(COL_LAST_MODIFIED)
                val timeIdx = it.getColumnIndexOrThrow(COL_INDEXED_AT)

                while (it.moveToNext()) {
                    val uri = it.getString(uriIdx)
                    result[uri] = IndexedFileRecord(
                        uri = uri,
                        fileName = it.getString(nameIdx),
                        mimeType = if (it.isNull(mimeIdx)) null else it.getString(mimeIdx),
                        size = it.getLong(sizeIdx),
                        lastModified = it.getLong(modIdx),
                        indexedAt = it.getLong(timeIdx)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading indexed files", e)
        }
        return result
    }

    /**
     * Removes all indexing records and segments for a specific file URI.
     */
    fun deleteFileIndex(fileUri: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FILES, "$COL_FILE_URI = ?", arrayOf(fileUri))
            db.delete(TABLE_FTS, "$FTS_COL_URI = ?", arrayOf(fileUri))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file index for $fileUri", e)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Saves a file record and batch-inserts all its extracted segments in a single fast transaction.
     */
    fun saveFileAndSegments(record: IndexedFileRecord, segments: List<FileSegment>) {
        val db = writableDatabase
        db.beginTransaction()
        var statement: SQLiteStatement? = null
        try {
            // 1. Delete previous records for this file if existing
            db.delete(TABLE_FILES, "$COL_FILE_URI = ?", arrayOf(record.uri))
            db.delete(TABLE_FTS, "$FTS_COL_URI = ?", arrayOf(record.uri))

            // 2. Insert into indexed_files
            val cv = ContentValues().apply {
                put(COL_FILE_URI, record.uri)
                put(COL_FILE_NAME, record.fileName)
                put(COL_MIME_TYPE, record.mimeType)
                put(COL_FILE_SIZE, record.size)
                put(COL_LAST_MODIFIED, record.lastModified)
                put(COL_INDEXED_AT, record.indexedAt)
            }
            db.insertWithOnConflict(TABLE_FILES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)

            // 3. Batch insert segments into FTS
            if (segments.isNotEmpty()) {
                val insertSql = """
                    INSERT INTO $TABLE_FTS ($FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT, $FTS_COL_NORMALIZED)
                    VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
                statement = db.compileStatement(insertSql)

                for (segment in segments) {
                    statement.clearBindings()
                    statement.bindString(1, segment.fileUri)
                    statement.bindString(2, segment.fileName)
                    statement.bindString(3, segment.location)
                    statement.bindString(4, segment.content)
                    statement.bindString(5, segment.normalizedContent)
                    statement.executeInsert()
                }
            }

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file and segments for ${record.fileName}", e)
        } finally {
            statement?.close()
            db.endTransaction()
        }
    }

    /**
     * Executes FTS search using normalized query terms.
     * Uses BM25 ranking when FTS5 is active, or standard FTS matching when FTS4 is active.
     */
    fun search(rawQuery: String, limit: Int = 150): List<SearchResult> {
        val normalizedQuery = ArabicNormalizer.normalize(rawQuery)
        if (normalizedQuery.isBlank()) return emptyList()

        // Clean tokens from any punctuation to guarantee valid FTS query syntax
        val tokens = normalizedQuery.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val results = mutableListOf<SearchResult>()
        val db = readableDatabase
        val fts5 = checkIsFts5(db)

        // FTS syntax:
        // For FTS5: "token"*
        // For FTS4: token*
        val matchExpr = if (fts5) {
            tokens.joinToString(" ") { "\"$it\"*" }
        } else {
            tokens.joinToString(" ") { "$it*" }
        }

        val sql = if (fts5) {
            """
            SELECT rowid, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
            FROM $TABLE_FTS
            WHERE $TABLE_FTS MATCH ?
            ORDER BY bm25($TABLE_FTS)
            LIMIT ?
            """.trimIndent()
        } else {
            """
            SELECT rowid, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
            FROM $TABLE_FTS
            WHERE $TABLE_FTS MATCH ?
            LIMIT ?
            """.trimIndent()
        }

        try {
            val cursor = db.rawQuery(sql, arrayOf(matchExpr, limit.toString()))
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow("rowid")
                val uriIdx = it.getColumnIndexOrThrow(FTS_COL_URI)
                val nameIdx = it.getColumnIndexOrThrow(FTS_COL_NAME)
                val locIdx = it.getColumnIndexOrThrow(FTS_COL_LOCATION)
                val contentIdx = it.getColumnIndexOrThrow(FTS_COL_CONTENT)

                while (it.moveToNext()) {
                    results.add(
                        SearchResult(
                            id = it.getLong(idIdx),
                            fileUri = it.getString(uriIdx),
                            fileName = it.getString(nameIdx),
                            location = it.getString(locIdx),
                            snippet = it.getString(contentIdx),
                            query = rawQuery.trim()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary FTS query failed for expr [$matchExpr], trying simple token match", e)
            try {
                // Fallback: simple bare token without asterisk
                val fallbackExpr = tokens.joinToString(" ")
                val fallbackSql = """
                    SELECT rowid, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
                    FROM $TABLE_FTS
                    WHERE $TABLE_FTS MATCH ?
                    LIMIT ?
                """.trimIndent()

                val cursor = db.rawQuery(fallbackSql, arrayOf(fallbackExpr, limit.toString()))
                cursor.use {
                    val idIdx = it.getColumnIndexOrThrow("rowid")
                    val uriIdx = it.getColumnIndexOrThrow(FTS_COL_URI)
                    val nameIdx = it.getColumnIndexOrThrow(FTS_COL_NAME)
                    val locIdx = it.getColumnIndexOrThrow(FTS_COL_LOCATION)
                    val contentIdx = it.getColumnIndexOrThrow(FTS_COL_CONTENT)

                    while (it.moveToNext()) {
                        results.add(
                            SearchResult(
                                id = it.getLong(idIdx),
                                fileUri = it.getString(uriIdx),
                                fileName = it.getString(nameIdx),
                                location = it.getString(locIdx),
                                snippet = it.getString(contentIdx),
                                query = rawQuery.trim()
                            )
                        )
                    }
                }
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Search fallback failed", fallbackEx)
            }
        }

        return results
    }

    /**
     * Clears all indexed data.
     */
    fun clearAll() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FILES, null, null)
            db.delete(TABLE_FTS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Returns the count of indexed files and segments safely.
     */
    fun getStats(): Pair<Int, Int> {
        var filesCount = 0
        var segmentsCount = 0

        try {
            val db = readableDatabase
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_FILES", null).use {
                if (it.moveToFirst()) filesCount = it.getInt(0)
            }
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_FTS", null).use {
                if (it.moveToFirst()) segmentsCount = it.getInt(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading stats", e)
        }

        return Pair(filesCount, segmentsCount)
    }
}
```

## app/src/main/java/com/example/fastfilesearch/model/FileModels.kt

```kotlin
package com.example.fastfilesearch.model

data class IndexedFileRecord(
    val uri: String,
    val fileName: String,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long,
    val indexedAt: Long = System.currentTimeMillis()
)

data class FileSegment(
    val fileUri: String,
    val fileName: String,
    val location: String,
    val content: String,
    val normalizedContent: String
)

data class SearchResult(
    val id: Long = 0,
    val fileUri: String,
    val fileName: String,
    val location: String,
    val snippet: String,
    val query: String
)
```

## app/src/main/java/com/example/fastfilesearch/parser/DocxParser.kt

```kotlin
package com.example.fastfilesearch.parser

import android.util.Xml
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

object DocxParser {

    /**
     * Extracts text from Word .docx files by reading word/document.xml directly
     * using streaming XmlPullParser, avoiding memory-heavy libraries.
     */
    fun parse(
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    parseDocumentXml(zis, fileUri, fileName, onSegmentFound)
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun parseDocumentXml(
        stream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var paragraphIndex = 0
        var insideParagraph = false
        val currentParagraphText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (name.equals("p", ignoreCase = true) || name.endsWith(":p")) {
                        insideParagraph = true
                        paragraphIndex++
                        currentParagraphText.setLength(0)
                    } else if (insideParagraph && (name.equals("t", ignoreCase = true) || name.endsWith(":t"))) {
                        val text = parser.nextText()
                        if (text.isNotEmpty()) {
                            currentParagraphText.append(text).append(" ")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name.equals("p", ignoreCase = true) || name.endsWith(":p")) {
                        insideParagraph = false
                        val text = currentParagraphText.toString().trim()
                        if (text.isNotEmpty()) {
                            val normalized = ArabicNormalizer.normalize(text)
                            if (normalized.isNotEmpty()) {
                                onSegmentFound(
                                    FileSegment(
                                        fileUri = fileUri,
                                        fileName = fileName,
                                        location = "فقرة $paragraphIndex",
                                        content = text,
                                        normalizedContent = normalized
                                    )
                                )
                            }
                        }
                        currentParagraphText.setLength(0)
                    }
                }
            }
            eventType = parser.next()
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/parser/ExcelXlsParser.kt

```kotlin
package com.example.fastfilesearch.parser

import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import jxl.Workbook
import java.io.InputStream

object ExcelXlsParser {

    /**
     * Extracts text from legacy Excel (.xls) files using JExcelApi (jxl).
     * Records sheet name and 1-based row number.
     */
    fun parse(
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val workbook = Workbook.getWorkbook(inputStream)
        try {
            for (sheet in workbook.sheets) {
                for (rowIndex in 0 until sheet.rows) {
                    val rowCells = sheet.getRow(rowIndex)
                    val cells = mutableListOf<String>()
                    for (cell in rowCells) {
                        val contents = cell?.contents?.trim()
                        if (!contents.isNullOrEmpty()) {
                            cells.add(contents)
                        }
                    }

                    if (cells.isNotEmpty()) {
                        val rowContent = cells.joinToString(" | ")
                        val normalized = ArabicNormalizer.normalize(rowContent)
                        if (normalized.isNotEmpty()) {
                            onSegmentFound(
                                FileSegment(
                                    fileUri = fileUri,
                                    fileName = fileName,
                                    location = "ورقة: ${sheet.name}، صف: ${rowIndex + 1}",
                                    content = rowContent,
                                    normalizedContent = normalized
                                )
                            )
                        }
                    }
                }
            }
        } finally {
            workbook.close()
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/parser/ExcelXlsxParser.kt

```kotlin
package com.example.fastfilesearch.parser

import android.content.Context
import android.util.Log
import android.util.Xml
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ExcelXlsxParser {

    private const val TAG = "ExcelXlsxParser"

    /**
     * Extracts text from modern Excel (.xlsx) files using a streaming, high-performance
     * OpenXML Zip + XmlPullParser implementation that does not depend on StAX (javax.xml.stream)
     * which is absent from Android's runtime.
     */
    fun parse(
        context: Context,
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val tempFile = File.createTempFile("xlsx_", ".tmp", context.cacheDir)
        try {
            tempFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }

            ZipFile(tempFile).use { zip ->
                // 1. Parse sheet names from xl/workbook.xml
                val sheetNames = parseSheetNames(zip)

                // 2. Parse shared strings from xl/sharedStrings.xml
                val sharedStrings = parseSharedStrings(zip)

                // 3. Find and sort sheet entries: xl/worksheets/sheet1.xml, sheet2.xml, ...
                val sheetEntries = mutableListOf<ZipEntry>()
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                        sheetEntries.add(entry)
                    }
                }

                sheetEntries.sortBy { entry ->
                    val num = entry.name.removePrefix("xl/worksheets/sheet").removeSuffix(".xml").toIntOrNull() ?: 0
                    num
                }

                // 4. Parse each sheet
                sheetEntries.forEachIndexed { index, sheetEntry ->
                    val sheetName = sheetNames.getOrNull(index) ?: "ورقة ${index + 1}"
                    parseSheetData(
                        zip = zip,
                        sheetEntry = sheetEntry,
                        sheetName = sheetName,
                        sharedStrings = sharedStrings,
                        fileUri = fileUri,
                        fileName = fileName,
                        onSegmentFound = onSegmentFound
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error parsing xlsx file: $fileName", t)
        } finally {
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (ignored: Exception) {}
        }
    }

    private fun parseSheetNames(zip: ZipFile): List<String> {
        val sheetNames = mutableListOf<String>()
        val workbookEntry = zip.getEntry("xl/workbook.xml") ?: return sheetNames

        try {
            zip.getInputStream(workbookEntry).use { stream ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType

                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val name = parser.name
                        if (name.equals("sheet", ignoreCase = true) || name.endsWith(":sheet")) {
                            val sheetName = parser.getAttributeValue(null, "name")
                            if (!sheetName.isNullOrBlank()) {
                                sheetNames.add(sheetName.trim())
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse sheet names from xl/workbook.xml", e)
        }

        return sheetNames
    }

    private fun parseSharedStrings(zip: ZipFile): List<String> {
        val sharedStrings = ArrayList<String>()
        val sstEntry = zip.getEntry("xl/sharedStrings.xml") ?: return sharedStrings

        try {
            zip.getInputStream(sstEntry).use { stream ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType
                var insideSi = false
                val currentString = StringBuilder()

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            val name = parser.name
                            if (name.equals("si", ignoreCase = true) || name.endsWith(":si")) {
                                insideSi = true
                                currentString.setLength(0)
                            } else if (insideSi && (name.equals("t", ignoreCase = true) || name.endsWith(":t"))) {
                                val text = parser.nextText()
                                currentString.append(text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val name = parser.name
                            if (name.equals("si", ignoreCase = true) || name.endsWith(":si")) {
                                insideSi = false
                                sharedStrings.add(currentString.toString().trim())
                                currentString.setLength(0)
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing shared strings", e)
        }

        return sharedStrings
    }

    private fun parseSheetData(
        zip: ZipFile,
        sheetEntry: ZipEntry,
        sheetName: String,
        sharedStrings: List<String>,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        try {
            zip.getInputStream(sheetEntry).use { stream ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType

                var currentRowNum = 0
                val rowCells = mutableListOf<String>()
                var currentCellType: String? = null
                val cellTextBuffer = StringBuilder()
                var insideCell = false
                var insideIs = false

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            val name = parser.name
                            when {
                                name.equals("row", ignoreCase = true) || name.endsWith(":row") -> {
                                    rowCells.clear()
                                    val rAttr = parser.getAttributeValue(null, "r")
                                    currentRowNum = rAttr?.toIntOrNull() ?: (currentRowNum + 1)
                                }
                                name.equals("c", ignoreCase = true) || name.endsWith(":c") -> {
                                    insideCell = true
                                    currentCellType = parser.getAttributeValue(null, "t")
                                    cellTextBuffer.setLength(0)
                                }
                                insideCell && (name.equals("is", ignoreCase = true) || name.endsWith(":is")) -> {
                                    insideIs = true
                                }
                                insideCell && (name.equals("t", ignoreCase = true) || name.endsWith(":t")) -> {
                                    val text = parser.nextText()
                                    cellTextBuffer.append(text)
                                }
                                insideCell && !insideIs && (name.equals("v", ignoreCase = true) || name.endsWith(":v")) -> {
                                    val text = parser.nextText()
                                    cellTextBuffer.append(text)
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val name = parser.name
                            when {
                                name.equals("c", ignoreCase = true) || name.endsWith(":c") -> {
                                    insideCell = false
                                    insideIs = false
                                    val rawVal = cellTextBuffer.toString().trim()
                                    if (rawVal.isNotEmpty()) {
                                        val resolvedVal = if (currentCellType == "s") {
                                            val idx = rawVal.toIntOrNull()
                                            if (idx != null && idx in 0 until sharedStrings.size) {
                                                sharedStrings[idx]
                                            } else {
                                                rawVal
                                            }
                                        } else {
                                            rawVal
                                        }

                                        if (resolvedVal.isNotBlank()) {
                                            rowCells.add(resolvedVal)
                                        }
                                    }
                                    cellTextBuffer.setLength(0)
                                    currentCellType = null
                                }
                                name.equals("row", ignoreCase = true) || name.endsWith(":row") -> {
                                    if (rowCells.isNotEmpty()) {
                                        val rowText = rowCells.joinToString(" | ")
                                        val normalized = ArabicNormalizer.normalize(rowText)
                                        if (normalized.isNotEmpty()) {
                                            onSegmentFound(
                                                FileSegment(
                                                    fileUri = fileUri,
                                                    fileName = fileName,
                                                    location = "ورقة: $sheetName، صف: $currentRowNum",
                                                    content = rowText,
                                                    normalizedContent = normalized
                                                )
                                            )
                                        }
                                    }
                                    rowCells.clear()
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing sheet ${sheetEntry.name}", e)
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/parser/FileParserDispatcher.kt

```kotlin
package com.example.fastfilesearch.parser

import android.content.Context
import com.example.fastfilesearch.model.FileSegment
import java.io.InputStream
import java.util.Locale

object FileParserDispatcher {

    private val SUPPORTED_EXTENSIONS = setOf(
        "xlsx", "xls", "docx", "pdf", "txt", "csv", "json", "md", "log"
    )

    fun isSupportedFile(fileName: String): Boolean {
        if (fileName.startsWith("~$")) return false
        val ext = getExtension(fileName)
        return SUPPORTED_EXTENSIONS.contains(ext)
    }

    fun getExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot != -1 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    /**
     * Parses the given stream based on its file extension and invokes [onSegmentFound] for every extracted segment.
     */
    fun parseFile(
        context: Context,
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val ext = getExtension(fileName)
        when (ext) {
            "xlsx" -> ExcelXlsxParser.parse(context, inputStream, fileUri, fileName, onSegmentFound)
            "xls" -> ExcelXlsParser.parse(inputStream, fileUri, fileName, onSegmentFound)
            "docx" -> DocxParser.parse(inputStream, fileUri, fileName, onSegmentFound)
            "pdf" -> PdfParser.parse(context, inputStream, fileUri, fileName, onSegmentFound)
            "txt", "csv", "json", "md", "log" -> TextFileParser.parse(inputStream, fileUri, fileName, onSegmentFound)
            else -> {
                // Unsupported format, skip safely
            }
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/parser/PdfParser.kt

```kotlin
package com.example.fastfilesearch.parser

import android.content.Context
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

object PdfParser {

    private val isInitialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            PDFBoxResourceLoader.init(context.applicationContext)
        }
    }

    /**
     * Extracts text from PDF files page-by-page using pdfbox-android.
     */
    fun parse(
        context: Context,
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        initialize(context)

        PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper()
            val totalPages = document.numberOfPages

            for (page in 1..totalPages) {
                stripper.startPage = page
                stripper.endPage = page
                val pageText = stripper.getText(document)

                if (pageText.isNotBlank()) {
                    for (line in pageText.lines()) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            val normalized = ArabicNormalizer.normalize(trimmed)
                            if (normalized.isNotEmpty()) {
                                onSegmentFound(
                                    FileSegment(
                                        fileUri = fileUri,
                                        fileName = fileName,
                                        location = "صفحة $page",
                                        content = trimmed,
                                        normalizedContent = normalized
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/parser/TextFileParser.kt

```kotlin
package com.example.fastfilesearch.parser

import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object TextFileParser {

    /**
     * Extracts text from plain text files (TXT, CSV, JSON, MD, LOG) line-by-line using UTF-8.
     */
    fun parse(
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).useLines { lines ->
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    val normalized = ArabicNormalizer.normalize(trimmed)
                    if (normalized.isNotEmpty()) {
                        onSegmentFound(
                            FileSegment(
                                fileUri = fileUri,
                                fileName = fileName,
                                location = "سطر ${index + 1}",
                                content = trimmed,
                                normalizedContent = normalized
                            )
                        )
                    }
                }
            }
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/ui/FileTypeHelper.kt

```kotlin
package com.example.fastfilesearch.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.fastfilesearch.parser.FileParserDispatcher

data class FileTypeVisual(
    val icon: ImageVector,
    val color: Color,
    val typeName: String
)

object FileTypeHelper {

    fun getVisual(fileName: String): FileTypeVisual {
        val ext = FileParserDispatcher.getExtension(fileName)
        return when (ext) {
            "xlsx", "xls" -> FileTypeVisual(
                icon = Icons.Default.TableChart,
                color = Color(0xFF1E7E34), // Excel Green
                typeName = if (ext == "xlsx") "Excel (XLSX)" else "Excel (XLS)"
            )
            "docx" -> FileTypeVisual(
                icon = Icons.Default.Description,
                color = Color(0xFF185ABD), // Word Blue
                typeName = "Word (DOCX)"
            )
            "pdf" -> FileTypeVisual(
                icon = Icons.Default.PictureAsPdf,
                color = Color(0xFFD32F2F), // PDF Red
                typeName = "PDF"
            )
            "txt", "log" -> FileTypeVisual(
                icon = Icons.AutoMirrored.Filled.Article,
                color = Color(0xFF455A64), // Text Slate
                typeName = ext.uppercase()
            )
            "csv" -> FileTypeVisual(
                icon = Icons.Default.TableChart,
                color = Color(0xFF00796B), // Teal
                typeName = "CSV"
            )
            "json" -> FileTypeVisual(
                icon = Icons.Default.Code,
                color = Color(0xFFE65100), // Orange
                typeName = "JSON"
            )
            "md" -> FileTypeVisual(
                icon = Icons.AutoMirrored.Filled.Article,
                color = Color(0xFF5E35B1), // Purple
                typeName = "Markdown"
            )
            else -> FileTypeVisual(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                color = Color(0xFF757575),
                typeName = ext.uppercase()
            )
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/ui/SearchScreen.kt

```kotlin
package com.example.fastfilesearch.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.SearchUiState
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.parser.FileParserDispatcher
import kotlinx.coroutines.launch

data class FileResultGroup(
    val fileUri: String,
    val fileName: String,
    val isDocument: Boolean,
    val results: List<SearchResult>
)

// Official Microsoft Excel Colors
private val ExcelDarkGreen = Color(0xFF107C41)
private val ExcelHeaderGray = Color(0xFFF3F2F1)
private val ExcelGridLine = Color(0xFFD4D4D4)
private val ExcelHeaderBorder = Color(0xFFC8C6C4)
private val ExcelHeaderText = Color(0xFF505050)
private val ExcelSelectedBg = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onSelectFolderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onOpenFile: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onToggleStretchSingleColumn: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onDismissMessage()
        }
    }

    // Settings Modal Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            uiState = uiState,
            onSelectFolderClick = onSelectFolderClick,
            onRefreshClick = onRefreshClick,
            onToggleStretchSingleColumn = onToggleStretchSingleColumn,
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Group results by file URI: Word/PDF use Method #1 (Grouped), Excel/Text use Method #4 (Official Excel Grid)
    val groupedResults = remember(uiState.searchResults) {
        uiState.searchResults
            .groupBy { it.fileUri }
            .map { (uri, results) ->
                val fileName = results.firstOrNull()?.fileName ?: ""
                FileResultGroup(
                    fileUri = uri,
                    fileName = fileName,
                    isDocument = TableCellHelper.isDocumentType(fileName),
                    results = results
                )
            }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (uiState.folderName.isNotBlank()) uiState.folderName else "البحث السريع في الملفات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (uiState.folderUri != null) {
                                    Text(
                                        text = "${uiState.filesCount} ملف مفهرس • ${uiState.segmentsCount} مقطع",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "الإعدادات",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            val isIndexing = uiState.indexProgress.isIndexing
                            val rotation = if (isIndexing) {
                                val infiniteTransition = rememberInfiniteTransition(label = "spin")
                                val angle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "spinAngle"
                                )
                                angle
                            } else 0f

                            IconButton(
                                onClick = onRefreshClick,
                                enabled = uiState.folderUri != null && !isIndexing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث الفهرس",
                                    modifier = Modifier.rotate(rotation),
                                    tint = if (uiState.folderUri != null && !isIndexing)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Indexing Progress Bar
                    AnimatedVisibility(visible = uiState.indexProgress.isIndexing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            val progress = uiState.indexProgress
                            if (progress.total > 0) {
                                LinearProgressIndicator(
                                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = progress.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Instant Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    placeholder = {
                        Text(
                            text = "ابحث",
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Content View depending on State
                when {
                    uiState.folderUri == null -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            EmptyFolderPrompt(onSelectFolderClick = onSelectFolderClick)
                        }
                    }

                    uiState.searchQuery.isBlank() -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            ReadyToSearchPrompt(
                                folderName = uiState.folderName,
                                filesCount = uiState.filesCount,
                                segmentsCount = uiState.segmentsCount,
                                onRefreshClick = onRefreshClick,
                                isIndexing = uiState.indexProgress.isIndexing
                            )
                        }
                    }

                    uiState.searchResults.isEmpty() && !uiState.isSearching -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            NoResultsPrompt(query = uiState.searchQuery)
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تم العثور على ${uiState.searchResults.size} نتيجة في ${groupedResults.size} ملف",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = groupedResults,
                                key = { it.fileUri }
                            ) { group ->
                                if (group.isDocument) {
                                    // Method #1: Grouped Card for Word (.docx) & PDF (.pdf) with padding
                                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                        WordPdfGroupedCard(
                                            group = group,
                                            onOpenFile = { onOpenFile(group.fileUri) },
                                            onCopySnippet = { text ->
                                                clipboardManager.setText(AnnotatedString(text))
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("تم نسخ الفقرة إلى الحافظة")
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    // Method #4: Pure flat data table replica matching screenshot (Edge-to-Edge)
                                    OfficialExcelTableCard(
                                        group = group,
                                        stretchSingleColumn = uiState.stretchSingleColumn,
                                        onOpenFile = { onOpenFile(group.fileUri) },
                                        onCopyRow = { text ->
                                            clipboardManager.setText(AnnotatedString(text))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("تم نسخ بيانات الصف إلى الحافظة")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Method #1: Grouped Document Card for Word (.docx) & PDF (.pdf).
 */
@Composable
fun WordPdfGroupedCard(
    group: FileResultGroup,
    onOpenFile: () -> Unit,
    onCopySnippet: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = FileTypeHelper.getVisual(group.fileName)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Document Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFile)
                    .background(visual.color.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(visual.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = visual.typeName,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${group.results.size} مطابقة في هذا المستند",
                        fontSize = 12.sp,
                        color = visual.color,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onOpenFile,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "فتح الملف",
                        tint = visual.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = visual.color.copy(alpha = 0.2f), thickness = 1.dp)

            // Document Snippets List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                group.results.forEach { result ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenFile)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(visual.color.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = visual.color,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = result.location,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = visual.color
                                )
                            }

                            IconButton(
                                onClick = { onCopySnippet(result.snippet) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ الفقرة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val highlightedSnippet = remember(result.snippet, result.query) {
                            TextHighlighter.highlight(result.snippet, result.query)
                        }

                        Text(
                            text = highlightedSnippet,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exact replica of the data table screen from the user's reference:
 * - Edge-to-edge dark table layout without any bulky cards, shadows, or outer paddings.
 * - Top header bar with file type icon, file name, and NO search/refresh icons.
 * - Table column header with ≡ menu icon above row numbers and "عمود 1", "عمود 2", "عمود 3"...
 * - Dark theme grid with row numbers on the side.
 * - Dropdown menu on row number click containing ONLY "نسخ" and "مشاركة".
 */
@Composable
fun OfficialExcelTableCard(
    group: FileResultGroup,
    stretchSingleColumn: Boolean = true,
    onOpenFile: () -> Unit,
    onCopyRow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visual = remember(group.fileName) { FileTypeHelper.getVisual(group.fileName) }

    // Multi-sheet support
    val sheetGroups = remember(group.results) {
        group.results.groupBy { result ->
            extractSheetAndRow(result.location).first
        }
    }

    val onShareRow: (String) -> Unit = remember(context) {
        { text ->
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "مشاركة بيانات الصف")
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    // Colors matching the dark screenshot exactly
    val topBarBg = Color(0xFF1C1F23)
    val colHeaderBg = Color(0xFF22262B)
    val tableBg = Color(0xFF131518)
    val rowAltBg = Color(0xFF16191D)
    val rowNumBg = Color(0xFF1B1E22)
    val gridBorderColor = Color(0xFF2D3137)
    val headerBorderColor = Color(0xFF33383F)
    val headerTextColor = Color(0xFFB5B9BF)
    val cellTextColor = Color(0xFFE4E7EB)
    val matchBg = Color(0xFF1A3822)
    val matchBorder = Color(0xFF388E3C)
    val popupBg = Color(0xFF202328)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tableBg)
    ) {
        // 1. Top Bar matching the screenshot: File type icon on right, file name, and Open File button on left
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBg)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon representing the file type
            Icon(
                imageVector = visual.icon,
                contentDescription = visual.typeName,
                tint = visual.color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // File Name in place of "جدول سريع"
            Text(
                text = group.fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Matches Count Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${group.results.size} صف",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Open File Button on the left (بدل زر البحث السابق في الصورة كما طلب المستخدم)
            IconButton(
                onClick = onOpenFile,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "فتح الملف",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = gridBorderColor, thickness = 1.dp)

        // 2. Render Sheet Tables (Flat and direct)
        sheetGroups.forEach { (sheetName, sheetResults) ->
            val rowsData = remember(sheetResults) {
                sheetResults.map { res ->
                    val (_, rowNum) = extractSheetAndRow(res.location)
                    val cells = TableCellHelper.splitIntoCells(res.snippet)
                    Triple(rowNum, cells, res)
                }
            }
            val maxCols = remember(rowsData) {
                rowsData.maxOfOrNull { it.second.size }?.coerceAtLeast(1) ?: 1
            }

            // Sheet Name Strip if more than 1 sheet
            if (sheetGroups.size > 1 || sheetName != "ورقة 1") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181B1F))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF107C41))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sheetName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    )
                }
                HorizontalDivider(color = gridBorderColor, thickness = 0.5.dp)
            }

            // State for which row number's DropdownMenu is open
            var menuExpandedRowIndex by remember { mutableStateOf<Int?>(null) }

            // 3. Exact Data Grid with Auto-fit Column Widths and Equal Row Height
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val totalWidth = maxWidth
                val rowNumWidth = 44.dp
                val remainingWidth = if (totalWidth > rowNumWidth) totalWidth - rowNumWidth else 0.dp

                // Calculate width for each column dynamically based on its longest content & words
                val columnWidths = remember(rowsData, maxCols, remainingWidth, stretchSingleColumn) {
                    val baseWidths = (0 until maxCols).map { colIndex ->
                        val maxChars = rowsData.maxOfOrNull {
                            it.second.getOrNull(colIndex)?.trim()?.length ?: 0
                        } ?: 0

                        val maxWordLength = rowsData.maxOfOrNull { row ->
                            val text = row.second.getOrNull(colIndex) ?: ""
                            text.split("\\s+".toRegex()).maxOfOrNull { it.length } ?: 0
                        } ?: 0

                        // Numbers/codes: ~80.dp - 95.dp
                        // Descriptions: ~170.dp - 210.dp
                        // Full names: ~220.dp - 280.dp
                        val estimatedDp = (maxChars * 9f + maxWordLength * 4f + 36).dp
                        estimatedDp.coerceIn(80.dp, 280.dp)
                    }

                    val totalBaseWidth = baseWidths.fold(0.dp) { acc, dp -> acc + dp }
                    if (maxCols == 1 && stretchSingleColumn) {
                        // Expand to 100% of the screen width!
                        listOf(remainingWidth)
                    } else if (totalBaseWidth < remainingWidth && totalBaseWidth > 0.dp && maxCols > 1) {
                        val ratio = remainingWidth.value / totalBaseWidth.value
                        baseWidths.map { (it.value * ratio).dp }
                    } else {
                        // Content width like Excel
                        baseWidths
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        // Column Header Row: [≡] | [عمود 1] | [عمود 2] | [عمود 3] | ...
                        Row(
                            modifier = Modifier
                                .background(colHeaderBg)
                                .border(0.5.dp, headerBorderColor)
                        ) {
                            // Right Corner: [≡] Menu icon above row numbers (matching screenshot)
                            Box(
                                modifier = Modifier
                                    .width(rowNumWidth)
                                    .height(38.dp)
                                    .border(0.5.dp, headerBorderColor)
                                    .background(colHeaderBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "قائمة",
                                    tint = headerTextColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Column Titles: with tailored auto-width per column
                            for (colIndex in 0 until maxCols) {
                                val colW = columnWidths.getOrElse(colIndex) { 110.dp }
                                Box(
                                    modifier = Modifier
                                        .width(colW)
                                        .height(38.dp)
                                        .border(0.5.dp, headerBorderColor)
                                        .background(colHeaderBg)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "عمود ${colIndex + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = headerTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Data Rows: Height expands to match the tallest cell in each row!
                        rowsData.forEachIndexed { index, (rowNum, cells, result) ->
                            val isEven = index % 2 == 0
                            val rowBg = if (isEven) tableBg else rowAltBg

                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Row Number Cell: fillMaxHeight() so it matches the tallest cell
                                Box(
                                    modifier = Modifier
                                        .width(rowNumWidth)
                                        .fillMaxHeight()
                                        .border(0.5.dp, headerBorderColor)
                                        .background(rowNumBg)
                                        .clickable { menuExpandedRowIndex = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rowNum,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = headerTextColor,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Popup Menu on Row Number click (ONLY "نسخ" and "مشاركة" as requested)
                                    DropdownMenu(
                                        expanded = menuExpandedRowIndex == index,
                                        onDismissRequest = { menuExpandedRowIndex = null },
                                        modifier = Modifier
                                            .background(popupBg)
                                            .border(0.5.dp, gridBorderColor, RoundedCornerShape(8.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "نسخ",
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "نسخ",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                onCopyRow(result.snippet)
                                                menuExpandedRowIndex = null
                                            }
                                        )
                                        HorizontalDivider(color = gridBorderColor, thickness = 0.5.dp)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "مشاركة",
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "مشاركة",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                onShareRow(result.snippet)
                                                menuExpandedRowIndex = null
                                            }
                                        )
                                    }
                                }

                                // Data Cells: fillMaxHeight() so every cell in the row has identical height
                                for (colIndex in 0 until maxCols) {
                                    val colW = columnWidths.getOrElse(colIndex) { 110.dp }
                                    val cellText = cells.getOrNull(colIndex) ?: ""
                                    val isMatch = remember(cellText, result.query) {
                                        TableCellHelper.isCellMatching(cellText, result.query)
                                    }
                                    val highlightedCellText = remember(cellText, result.query) {
                                        TextHighlighter.highlight(cellText, result.query)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(colW)
                                            .fillMaxHeight()
                                            .background(if (isMatch) matchBg else Color.Transparent)
                                            .border(
                                                width = if (isMatch) 1.5.dp else 0.5.dp,
                                                color = if (isMatch) matchBorder else gridBorderColor,
                                                shape = RectangleShape
                                            )
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = highlightedCellText,
                                            fontSize = 13.sp,
                                            color = cellTextColor,
                                            fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal,
                                            lineHeight = 18.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = gridBorderColor, thickness = 1.dp)
        }
    }
}

/**
 * Extracts sheet name and row number from location string.
 */
private fun extractSheetAndRow(location: String): Pair<String, String> {
    var sheet = "ورقة 1"
    var row = "1"

    if (location.contains("ورقة:")) {
        val parts = location.split("،")
        sheet = parts.getOrNull(0)?.substringAfter("ورقة:")?.trim() ?: "ورقة 1"
        row = parts.getOrNull(1)?.replace("صف:", "")?.replace("صف", "")?.trim() ?: "1"
    } else if (location.contains("سطر")) {
        sheet = "بيانات جدولية"
        row = location.replace("سطر", "").replace(":", "").trim()
    } else {
        row = location.filter { it.isDigit() }.ifEmpty { "1" }
    }

    return Pair(sheet, row)
}

/**
 * Returns Excel column letters: 0 -> A, 1 -> B, 25 -> Z, 26 -> AA, 27 -> AB...
 */
private fun getExcelColumnLetter(index: Int): String {
    var n = index
    val sb = StringBuilder()
    while (n >= 0) {
        sb.insert(0, ('A'.code + (n % 26)).toChar())
        n = n / 26 - 1
    }
    return sb.toString()
}

@Composable
fun EmptyFolderPrompt(
    onSelectFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "اختر مجلداً للبدء",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "قم بتحديد المجلد الذي يحتوي على مستنداتك وسيقوم التطبيق بفهرستها ومزامنتها تلقائياً للبحث الفوري دون إنترنت.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FilledTonalButton(
            onClick = onSelectFolderClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "تحديد مجلد المستندات", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReadyToSearchPrompt(
    folderName: String,
    filesCount: Int,
    segmentsCount: Int,
    onRefreshClick: () -> Unit,
    isIndexing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "جاهز للبحث الفوري",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "المجلد الحالي: $folderName\nتمت فهرسة $filesCount ملف بنجاح ($segmentsCount مقطع جاهز)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Supported Formats Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatBadge(name = "Excel (جداول رسمية)", color = ExcelDarkGreen)
            FormatBadge(name = "Word (تجميعي)", color = Color(0xFF185ABD))
            FormatBadge(name = "PDF (تجميعي)", color = Color(0xFFD32F2F))
            FormatBadge(name = "نصوص وجداول", color = Color(0xFFE65100))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "يدعم البحث الفوري بجميع صيغ الحروف العربية (أ/إ/آ، ة/ه، ى/ي) ومع أو بدون التشكيل.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FormatBadge(name: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun NoResultsPrompt(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "لم يتم العثور على نتائج",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "لا توجد أي مطابقة للكلمة \"$query\" داخل الملفات المفهرسة.\nجرب كلمة أخرى أو تأكد من مزامنة المجلد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Settings Dialog for folder management and table display preferences.
 */
@Composable
fun SettingsDialog(
    uiState: SearchUiState,
    onSelectFolderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleStretchSingleColumn: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "الإعدادات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Folder Selection & Management
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مجلد البحث المفهرس",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (uiState.folderName.isNotBlank())
                            "المجلد الحالي: ${uiState.folderName} (${uiState.filesCount} ملف مفهرس)"
                        else
                            "لم يتم اختيار مجلد بعد",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                onDismiss()
                                onSelectFolderClick()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "تغيير المجلد", fontSize = 12.sp)
                        }

                        if (uiState.folderUri != null) {
                            FilledTonalButton(
                                onClick = {
                                    onRefreshClick()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Section 2: Single Column Files Stretch Toggle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تمديد الملفات ذات العمود الواحد (مثل txt)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.stretchSingleColumn)
                                    "مفعل: يتمدد لملء 100% من عرض الشاشة"
                                else
                                    "معطل: العرض حسب طول المحتوى مع التمرير كالإكسل",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = uiState.stretchSingleColumn,
                            onCheckedChange = { onToggleStretchSingleColumn(it) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}
```

## app/src/main/java/com/example/fastfilesearch/ui/TableCellHelper.kt

```kotlin
package com.example.fastfilesearch.ui

import com.example.fastfilesearch.parser.FileParserDispatcher
import com.example.fastfilesearch.util.ArabicNormalizer

object TableCellHelper {

    /**
     * Returns true if the file is a Word (.docx) or PDF (.pdf) document.
     */
    fun isDocumentType(fileName: String): Boolean {
        val ext = FileParserDispatcher.getExtension(fileName)
        return ext == "docx" || ext == "pdf"
    }

    /**
     * Determines whether the result should be displayed as a tabular grid of cells.
     */
    fun isTabular(fileName: String, content: String): Boolean {
        val ext = FileParserDispatcher.getExtension(fileName)
        if (ext in listOf("xlsx", "xls", "csv")) return true
        if (content.contains(" | ") || content.contains("\t")) return true
        if (content.count { it == ',' } >= 2) return true
        if (content.count { it == ';' } >= 2) return true
        if (content.count { it == '|' } >= 2) return true
        return false
    }

    /**
     * Splits a row into individual cell values based on common delimiters.
     */
    fun splitIntoCells(content: String): List<String> {
        val rawCells = when {
            content.contains(" | ") -> content.split(" | ")
            content.contains("\t") -> content.split("\t")
            content.count { it == ';' } >= 2 -> content.split(";")
            content.count { it == ',' } >= 2 -> content.split(",")
            content.contains("|") -> content.split("|")
            else -> listOf(content)
        }

        val cleaned = rawCells.map { it.trim() }.filter { it.isNotEmpty() }
        return if (cleaned.isNotEmpty()) cleaned else listOf(content)
    }

    /**
     * Checks if a specific cell matches any token in the search query using Arabic normalization.
     */
    fun isCellMatching(cell: String, query: String): Boolean {
        if (query.isBlank() || cell.isBlank()) return false
        val normalizedCell = ArabicNormalizer.normalize(cell)
        val normalizedQuery = ArabicNormalizer.normalize(query)
        val tokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        return tokens.any { normalizedCell.contains(it) }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/ui/TextHighlighter.kt

```kotlin
package com.example.fastfilesearch.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.fastfilesearch.util.ArabicNormalizer

object TextHighlighter {

    /**
     * Highlights search terms inside the original text by matching normalized tokens,
     * so variations like (أ / إ / آ / ا, ة / ه, ى / ي, تشكيل) are accurately highlighted.
     */
    fun highlight(
        text: String,
        query: String,
        highlightColor: Color = Color(0xFFFFD54F),
        highlightTextColor: Color = Color(0xFF1A1A1A)
    ): AnnotatedString {
        if (text.isEmpty() || query.isBlank()) {
            return AnnotatedString(text)
        }

        val normalizedQuery = ArabicNormalizer.normalize(query)
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (queryTokens.isEmpty()) {
            return AnnotatedString(text)
        }

        // Tokenize original text into words while keeping track of original offsets
        val words = text.split(Regex("(?<=\\s)|(?=\\s)|(?<=[^\\p{L}\\p{Nd}])|(?=[^\\p{L}\\p{Nd}])"))
        val matchedRanges = mutableListOf<IntRange>()

        var currentOffset = 0
        for (word in words) {
            val length = word.length
            val normalizedWord = ArabicNormalizer.normalize(word)

            if (normalizedWord.isNotEmpty()) {
                val matches = queryTokens.any { token ->
                    normalizedWord.contains(token)
                }
                if (matches) {
                    matchedRanges.add(currentOffset until (currentOffset + length))
                }
            }
            currentOffset += length
        }

        return buildAnnotatedString {
            append(text)
            for (range in matchedRanges) {
                if (range.first in 0 until text.length && range.last < text.length) {
                    addStyle(
                        style = SpanStyle(
                            background = highlightColor,
                            color = highlightTextColor,
                            fontWeight = FontWeight.Bold
                        ),
                        start = range.first,
                        end = range.last + 1
                    )
                }
            }
        }
    }
}
```

## app/src/main/java/com/example/fastfilesearch/ui/theme/Color.kt

```kotlin
package com.example.fastfilesearch.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

## app/src/main/java/com/example/fastfilesearch/ui/theme/Theme.kt

```kotlin
package com.example.fastfilesearch.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun FastFileSearchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

## app/src/main/java/com/example/fastfilesearch/ui/theme/Type.kt

```kotlin
package com.example.fastfilesearch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
```

## app/src/main/java/com/example/fastfilesearch/util/ArabicNormalizer.kt

```kotlin
package com.example.fastfilesearch.util

object ArabicNormalizer {

    /**
     * Normalizes Arabic text for uniform search matching:
     * - Strips all Tashkeel (Fatha, Damma, Kasra, Sukun, Shadda, Tanween, etc.)
     * - Strips Tatweel (Kashida 'ـ')
     * - Normalizes Alif variants (أ, إ, آ, ٱ) -> ا
     * - Normalizes Alef Maksura (ى) -> ي
     * - Normalizes Ta Marbuta (ة) -> ه
     * - Normalizes Persian/Urdu variants (ک -> ك, ی -> ي)
     * - Converts to lowercase and normalizes whitespace
     */
    fun normalize(text: String): String {
        if (text.isEmpty()) return ""

        val sb = StringBuilder(text.length)
        var lastWasSpace = false

        for (char in text) {
            when (char) {
                // Diacritics (Harakat) & Tatweel -> Skip
                '\u064B', // Fathatan
                '\u064C', // Dammatan
                '\u064D', // Kasratan
                '\u064E', // Fatha
                '\u064F', // Damma
                '\u0650', // Kasra
                '\u0651', // Shadda
                '\u0652', // Sukun
                '\u0653', // Maddah above
                '\u0654', // Hamza above
                '\u0655', // Hamza below
                '\u0670', // Superscript Alef
                '\u0640'  // Tatweel / Kashida
                -> {
                    // Do nothing (strip)
                }

                // Alif variants -> Bare Alif
                '\u0622', // آ
                '\u0623', // أ
                '\u0625', // إ
                '\u0671'  // ٱ
                -> {
                    sb.append('\u0627')
                    lastWasSpace = false
                }

                // Alef Maksura -> Ya
                '\u0649' // ى
                -> {
                    sb.append('\u064A') // ي
                    lastWasSpace = false
                }

                // Ta Marbuta -> Ha
                '\u0629' // ة
                -> {
                    sb.append('\u0647') // ه
                    lastWasSpace = false
                }

                // Persian/Urdu variants
                '\u06A9' // ک -> ك
                -> {
                    sb.append('\u0643')
                    lastWasSpace = false
                }
                '\u06CC' // ی -> ي
                -> {
                    sb.append('\u064A')
                    lastWasSpace = false
                }

                else -> {
                    if (char.isWhitespace()) {
                        if (!lastWasSpace && sb.isNotEmpty()) {
                            sb.append(' ')
                            lastWasSpace = true
                        }
                    } else {
                        sb.append(char.lowercaseChar())
                        lastWasSpace = false
                    }
                }
            }
        }

        return sb.toString().trim()
    }
}
```

## app/src/test/java/com/example/fastfilesearch/ArabicNormalizerTest.kt

```kotlin
package com.example.fastfilesearch

import com.example.fastfilesearch.util.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicNormalizerTest {

    @Test
    fun testAlifNormalization() {
        assertEquals("احمد", ArabicNormalizer.normalize("أحمد"))
        assertEquals("احمد", ArabicNormalizer.normalize("إحمد"))
        assertEquals("اثار", ArabicNormalizer.normalize("آثار"))
        assertEquals("اسم", ArabicNormalizer.normalize("ٱسم"))
    }

    @Test
    fun testAlefMaksuraAndTaMarbuta() {
        assertEquals("علي", ArabicNormalizer.normalize("على"))
        assertEquals("شركه", ArabicNormalizer.normalize("شركة"))
        assertEquals("مدينه", ArabicNormalizer.normalize("مدينة"))
    }

    @Test
    fun testDiacriticsAndTatweelRemoval() {
        val withTashkeel = "مُحَمَّدٌ"
        assertEquals("محمد", ArabicNormalizer.normalize(withTashkeel))

        val withTatweel = "تـــــقــــريـــــر"
        assertEquals("تقرير", ArabicNormalizer.normalize(withTatweel))

        val mixed = "شَــــرِكَةُ أَعْمَالٍ"
        assertEquals("شركه اعمال", ArabicNormalizer.normalize(mixed))
    }

    @Test
    fun testPersianVariants() {
        assertEquals("بانك", ArabicNormalizer.normalize("بانک"))
        assertEquals("علي", ArabicNormalizer.normalize("على"))
    }
}
```

## app/src/test/java/com/example/fastfilesearch/ExampleUnitTest.kt

```kotlin
package com.example.fastfilesearch

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
```

