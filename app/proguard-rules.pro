# --- Kosh Production Proguard Rules ---

# 1. Protect LiteRT (TensorFlow Lite) Native Libraries
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-keepnames class com.google.ai.edge.litertlm.**
-keepnames class com.google.ai.edge.litert.**

# 2. Prevent shrinking of JNI / Native symbols required for NPU access
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. Maintain Metadata for Document Parsing (PDFBox/Jsoup)
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.jsoup.** { *; }

# 4. Standard Android Library Protections
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-keepattributes Signature,AnnotationDefault,EnclosingMethod,InnerClasses
