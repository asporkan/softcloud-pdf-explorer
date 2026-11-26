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

# PDF Viewer - WebView JavaScript interface (JWI): yalnızca @JavascriptInterface yöntemleri adıyla kalmalı
-keepclassmembers class com.rejowan.pdfreaderpro.presentation.components.pdf.WebInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# ==========================================
# Logging - Strip in release builds
# ==========================================

# Remove Timber logging calls
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Remove Android Log calls (in case any direct calls exist)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# ==========================================
# Room / Kotlin Serialization / Koin
# ==========================================
# Room (room-runtime), kotlinx.serialization (core) ve Koin kendi consumer kurallarını AAR/JAR içinde
# getirir; uygulama paketi çapında ek -keep kuralı gerekmez.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ==========================================
# iText PDF (yalnızca yansımayla yüklenen sınıflar)
# ==========================================
# BouncyCastleFactoryCreator fabrikayı Class.forName ile ada göre yükler
# iText relies on its own class/package names at runtime:
#  - AbstractITextEvent rejects subclasses whose name does not start with "com.itextpdf".
#  - ContextManager maps classes to products by package prefix (com.itextpdf.kernel, .forms, ...).
# Keep names (and packages) only; unused iText classes are still removed and code is still optimized.
-keepnames class com.itextpdf.**
# Loaded via Class.forName in DIContainerConfigurations; their static init registers default
# DIContainer suppliers (otherwise: Supplier.get() on null in every iText tool).
-keep class com.itextpdf.kernel.utils.RegisterDefaultDiContainer { *; }
-keep class com.itextpdf.forms.util.RegisterDefaultDiContainer { *; }
-keep class com.itextpdf.bouncycastle.** { *; }
-keep class com.itextpdf.bouncycastleconnector.** { *; }

# ==========================================
# BouncyCastle (JCA sağlayıcısı algoritmaları sınıf adıyla yükler)
# ==========================================
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

# iText - Ignore missing Java SE classes (not available on Android)
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.xml.stream.**
-dontwarn com.itextpdf.bouncycastlefips.**
-dontwarn com.itextpdf.eutrustedlistsresources.**
-dontwarn sharpen.config.**
-dontwarn aQute.bnd.annotation.**
-dontwarn org.codehaus.stax2.**
-dontwarn com.ctc.wstx.**
