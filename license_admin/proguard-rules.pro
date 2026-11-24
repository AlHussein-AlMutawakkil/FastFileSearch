# Proguard rules for FastFileSearch (:license_admin)

# License Generation Models & Engine
-keep class com.example.fastfilesearch.licenseadmin.engine.** { *; }
-dontwarn com.example.fastfilesearch.licenseadmin.engine.**

# License History Management & Items
-keep class com.example.fastfilesearch.licenseadmin.history.** { *; }
-dontwarn com.example.fastfilesearch.licenseadmin.history.**

# Key Storage and Security
-keep class com.example.fastfilesearch.licenseadmin.security.** { *; }
-dontwarn com.example.fastfilesearch.licenseadmin.security.**

# Room Database, Entities, and DAOs
-keep class com.example.fastfilesearch.licenseadmin.data.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# WorkManager & Silent Supabase Sync
-keep class com.example.fastfilesearch.licenseadmin.sync.** { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.fastfilesearch.licenseadmin.BuildConfig { *; }
-keep class com.example.fastfilesearch.licenseadmin.util.** { *; }
