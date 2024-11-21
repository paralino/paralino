# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-keep class com.cossacklabs.themis.**
-keep class com.cossacklabs.themis.** {*;}

-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }
-keep class java.awt.* { *; }
-keepclassmembers class * extends java.awt.* { public *; }

-dontwarn org.jetbrains.annotations.**
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window

#retaning R.java resources names
-keepclassmembers class **.R$* {
    public static <fields>;
}

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# retaining fields of Serlizable class (including Enums and kotlin data classes)
-keepnames class app.paralino.**
-keepclassmembers class app.paralino.** implements java.io.Serializable {*;}
-keep class app.paralino.android.data.** { *; }
-keep class app.paralino.android.remote.** { *; }
-keep class app.paralino.core.models.** { *; }
-keep class app.paralino.core.db.** { *; }
-keep class io.appwrite.** { *; }

-keep class app.paralino.core.MokoResources { *; }
-keep class app.paralino.core.MokoResources$images { *; }
-keep class app.paralino.core.MokoResources$fonts { *; }
-keep class app.paralino.core.MokoResources$strings { *; }
-keep class app.paralino.core.MR { *; }
-keep class app.paralino.core.MR$fonts { *; }

# keep parcelables
-keepclassmembers class * implements android.os.Parcelable {
   public static final android.os.Parcelable$Creator *;
}


# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault


#Lifecycle
-keep class androidx.activity.** { *; }
-keep class androidx.lifecycle.** { *; }

# AboutLibraries
-keepclasseswithmembers class **.R$* {
    public static final int define_*;
}

# region added from missing_rules file
-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant

-dontwarn org.slf4j.impl.StaticLoggerBinder
# endregion

# region gson
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
# endregion

#create a merged proguard file of all modules and default proguard rules
-printconfiguration proguard-merged-config.txt