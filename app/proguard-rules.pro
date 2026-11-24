# Proguard rules for FastFileSearch (:app)

# Apache POI, XMLBeans, and OpenXMLFormats for Excel and Word parsing & export
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

-keep class org.openxmlformats.** { *; }
-dontwarn org.openxmlformats.**

-keep class org.apache.commons.** { *; }
-dontwarn org.apache.commons.**

-keep class javax.xml.** { *; }
-dontwarn javax.xml.**

-keep class org.w3c.dom.** { *; }
-dontwarn org.w3c.dom.**

# Licensing Models, Verifier, and Storage
-keep class com.example.fastfilesearch.licensing.** { *; }
-dontwarn com.example.fastfilesearch.licensing.**

# SQLite and Room / FTS
-keep class androidx.sqlite.** { *; }
-dontwarn androidx.sqlite.**

# PDFBox Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# JXL (Legacy Excel .xls)
-keep class jxl.** { *; }
-dontwarn jxl.**

# Device Info & Hardware Fingerprint (MediaDrm)
-keep class com.example.fastfilesearch.util.ClientDeviceIdProvider { *; }
-keep class com.example.fastfilesearch.util.DeviceInfoHelper { *; }
-dontwarn android.media.MediaDrm
-dontwarn android.media.MediaDrm$*
