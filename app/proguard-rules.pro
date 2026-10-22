# Add project specific ProGuard rules here.
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

# ML Kit barcode scanning: R8's optimizations break ML Kit's internal wiring in release builds
# (BarcodeScanning.getClient fails with "Attempt to read from field ... on a null object
# reference" and the camera preview stays black). Keep all of ML Kit and its internals as they
# are: keeping only some packages breaks its component wiring at start-up ("Unsatisfied
# dependency for component").
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }

# Retrofit reads each API method's generic return type (Response<T>) at runtime. A response class
# the app never reads (e.g. the result of incrementInventory/decrementInventory) is removed by R8,
# which then rewrites the signature to Response<Object>: the call fails before any request is sent
# ("Serializer for class 'Any' is not found"). Keep the API interface with its signatures and all
# serializable model classes; names may still be shortened.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowobfuscation interface org.librecellar.data.api.WineApiService { *; }
-keep,allowobfuscation @kotlinx.serialization.Serializable class org.librecellar.data.model.** { *; }
