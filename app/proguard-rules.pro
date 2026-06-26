# Room
# Room usually provides its own rules, but keep these if specific schema migrations or
# custom database configurations are used.
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase

# Keep entities - Ensures that Room can find and use entity fields after obfuscation.
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

# Compose, CameraX, and newer Coroutines rules are handled by the libraries' AARs.
# Broad keep rules for these are unnecessary and discouraged as they inflate the binary.

# SQLCipher
# SQLCipher uses JNI and requires keeping its classes for proper encryption support.
# Scoped to the database package and specific members to avoid overly broad rules.
-keep class net.zetetic.database.sqlcipher.SupportOpenHelperFactory { *; }
-keepclasseswithmembernames class net.zetetic.database.sqlcipher.** {
    native <methods>;
}
-dontwarn net.zetetic.**

# SQLCipher requires the Support SQLite interfaces to remain unobfuscated.
-keep class androidx.sqlite.db.SupportSQLite* { *; }

# Zip4j
# Usually fine with defaults, but if you encounter issues with encryption/decryption
# after obfuscation, uncomment the following:
# -keep class net.lingala.zip4j.** { *; }
