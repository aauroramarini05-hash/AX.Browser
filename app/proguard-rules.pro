# Add your custom ProGuard rules here.
# This file controls how code is obfuscated and shrunk for release builds.
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static *** databaseBuilder(...);
}
-keepattributes *Annotation*