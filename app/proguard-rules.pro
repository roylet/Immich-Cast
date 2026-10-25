# kotlinx.serialization: keep generated serializers for our DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.roylet.immichcast.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.roylet.immichcast.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp optional platform integrations.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
