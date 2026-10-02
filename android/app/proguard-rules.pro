# Security Hardening & Obfuscation Rules for Lloyd Attendance

# Keep data models serialized by Gson
-keep class com.lloyd.attendance.api.Models$* { <fields>; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Strip all debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Obfuscate internal implementation classes
-repackageclasses 'com.lloyd.attendance.internal'
-allowaccessmodification

# Prevent decompilers from easily reading stack traces & attributes
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
