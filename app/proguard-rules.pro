# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Firestore model classes (Firestore uses reflection to serialize/deserialize)
-keepattributes *Annotation*
-keepclassmembers class com.gdgcorehq.app.model.** {
    public <methods>;
    public <fields>;
}
-keep class com.gdgcorehq.app.model.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Keep CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
