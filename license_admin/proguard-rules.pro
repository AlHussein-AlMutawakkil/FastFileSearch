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
