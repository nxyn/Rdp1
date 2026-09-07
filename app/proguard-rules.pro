-keepattributes *Annotation*
-dontwarn com.google.errorprone.annotations.**
-keepclassmembers class * {
    @org.jetbrains.kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class com.nxyn.aiclient.**$$serializer { *; }
-keepclassmembers class com.nxyn.aiclient.** {
    *** Companion;
}
-keepclasseswithmembers class com.nxyn.aiclient.** {
    kotlinx.serialization.KSerializer serializer(...);
}
