# Add project specific ProGuard rules here.

# Release builds must not retain app or embedded-SDK log calls. This removes
# sensitive diagnostic arguments as well as the calls themselves.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Joda-Time references these optional conversion annotations only as metadata.
-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# The app's own JSON DTOs carry no @SerializedName, so field names must survive
# minification for Gson/Retrofit to round-trip the bank API payloads.
-keep class ng.wimika.samplebankapp.loginRepo.models.** { *; }
-keep class ng.wimika.samplebankapp.network.** { *; }
-keep class ng.wimika.samplebankapp.ui.screens.claims.** { *; }
# Retrofit's canonical R8 full-mode rule: keep any @retrofit2.http-annotated interface.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep interface <1> { *; }
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
