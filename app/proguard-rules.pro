# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.futbolmenajer.**$$serializer { *; }
-keepclassmembers class com.futbolmenajer.** { *** Companion; }
-keepclasseswithmembers class com.futbolmenajer.** { kotlinx.serialization.KSerializer serializer(...); }
-keep class com.futbolmenajer.data.model.** { *; }
-keepclassmembers enum com.futbolmenajer.** { *; }
-dontwarn org.slf4j.**
