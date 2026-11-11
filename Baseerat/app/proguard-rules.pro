# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep data classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Compose
-dontwarn androidx.compose.**
