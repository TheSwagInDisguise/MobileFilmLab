# Proguard rules for MobileFilmLab
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class com.google.gson.** { *; }
-keep class org.yaml.snakeyaml.** { *; }
