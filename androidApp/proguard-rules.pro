# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.cnsoftware.waveform.**$$serializer { *; }
-keepclassmembers class com.cnsoftware.waveform.** {
    *** Companion;
}
-keepclasseswithmembers class com.cnsoftware.waveform.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Manifest & WorkManager bilesenleri
-keep class com.cnsoftware.waveform.MainActivity { *; }
-keep class com.cnsoftware.waveform.shared.player.PodcastPlaybackService { *; }
-keep class com.cnsoftware.waveform.shared.data.SubscriptionRefreshWorker { *; }

# Room / WorkManager (R8 full mode: room-runtime 2.6.1 kurali parametresiz kurucuyu korumuyor)
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
