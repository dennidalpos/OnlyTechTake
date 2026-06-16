# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase

# Keep entities
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
    @androidx.room.Ignore *;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$HandlerPost {
    *** run();
}

# Compose
-keep class androidx.compose.ui.platform.** { *; }

# CameraX
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }

# SQLCipher
-keep class net.zetetic.** { *; }
-keep class androidx.sqlite.db.** { *; }
-keep class androidx.sqlite.db.framework.** { *; }
