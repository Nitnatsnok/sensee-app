# JNA binds Win32 functions reflectively (interfaces, callbacks, structures).
# ProGuard must not rename or strip these or the custom window chrome breaks.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
