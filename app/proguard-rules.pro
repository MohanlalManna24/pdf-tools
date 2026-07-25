# Keep OpenCV Native Classes & JNI Bridges
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Keep Room Database Entities & DAOs
-keep class com.example.data.db.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Keep OkHttp & Coroutines
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
