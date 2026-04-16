# Choreboo Habit Tracker R8/ProGuard Configuration
# Comprehensive keep rules for minification and shrinking

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Kotlin metadata for reflection
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ============ ROOM DATABASE ============
# Keep all Room entities, DAOs, and database classes
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Keep Room type converters
-keep class com.choreboo.app.data.local.converter.** { *; }

# ============ HILT / DAGGER ============
# Keep Hilt and Dagger annotations and generated code
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.qualifiers.** class * { *; }
-keep @javax.inject.Inject class * { *; }
-keep @javax.inject.Qualifier class * { *; }
-keep @javax.inject.Scope class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.Provides class * { <methods>; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.**
-dontwarn javax.inject.**

# Keep Hilt ViewModel
-keep @androidx.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ============ GSON / JSON SERIALIZATION ============
# Keep Gson TypeAdapters and generated code
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-dontwarn com.google.gson.**

# Keep all domain models and entities (used by Gson reflection)
-keep class com.choreboo.app.domain.model.** { *; }
-keep class com.choreboo.app.data.local.entity.** { *; }

# ============ FIREBASE DATA CONNECT GENERATED SDK ============
# Keep all Data Connect generated classes and interfaces
-keep class com.google.firestore.v1.** { *; }
-keep class com.google.firestore.admin.v1.** { *; }
-keep interface com.google.firestore.v1.** { *; }
-keep interface com.google.firestore.admin.v1.** { *; }
-dontwarn com.google.firestore.v1.**
-dontwarn com.google.firestore.admin.v1.**

# Keep Firebase Data Connect client classes
-keep class com.google.firebase.dataconnect.** { *; }
-keep interface com.google.firebase.dataconnect.** { *; }
-dontwarn com.google.firebase.dataconnect.**

# Keep gRPC classes (used by Data Connect)
-keep class io.grpc.** { *; }
-keep interface io.grpc.** { *; }
-dontwarn io.grpc.**

# Keep Protobuf (used by gRPC/Data Connect)
-keep class com.google.protobuf.** { *; }
-keep interface com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# ============ FIREBASE AUTHENTICATION ============
# Keep Firebase Auth classes
-keep class com.google.firebase.auth.** { *; }
-keep interface com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# ============ ANDROID CREDENTIAL MANAGER ============
# Keep Credential Manager API classes (Android 14+)
-keep class androidx.credentials.** { *; }
-keep interface androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

# ============ JETPACK COMPOSE ============
# Keep Compose Runtime classes
-keep class androidx.compose.runtime.** { *; }
-keep interface androidx.compose.runtime.** { *; }

# Keep Material3 and Compose Material classes
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }
-keep interface androidx.compose.material3.** { *; }
-keep interface androidx.compose.material.** { *; }

# Keep Compose UI classes
-keep class androidx.compose.ui.** { *; }
-keep interface androidx.compose.ui.** { *; }
-dontwarn androidx.compose.ui.**

# ============ ANDROIDX LIFECYCLE ============
# Keep Lifecycle and ViewModel classes
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }
-keep class androidx.lifecycle.viewmodel.** { *; }
-dontwarn androidx.lifecycle.**

# ============ NAVIGATION ============
# Keep Navigation Compose classes
-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ============ ANDROIDX CORE ============
# Keep core Android Framework extensions
-keep class androidx.core.** { *; }
-keep interface androidx.core.** { *; }
-dontwarn androidx.core.**

# ============ KOTLIN SERIALIZATION ============
# Keep Kotlin serialization runtime and generated serializers
-keep class kotlinx.serialization.** { *; }
-keep class * extends kotlinx.serialization.KSerializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-dontwarn kotlinx.serialization.**

# ============ OKHTTP3 ============
# Keep OkHttp classes (used by Coil and Firebase)
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# ============ RETROFIT2 ============
# Keep Retrofit2 classes if used
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

# ============ COIL IMAGE LOADING ============
# Keep Coil classes
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ============ GOOGLE PLAY SERVICES ============
# Keep Google Play Services classes
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============ GOOGLE PLAY BILLING ============
# Keep Google Play Billing classes
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ============ GOOGLE ADMOB ============
# Keep Google AdMob classes
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ============ TIMBER LOGGING ============
# Keep Timber classes
-keep class timber.log.Timber { *; }
-keep class timber.log.Timber$Tree { *; }
-dontwarn timber.**

# ============ PROJECT-SPECIFIC CLASSES ============
# Keep all app classes to be safe during development
-keep class com.choreboo.app.** { *; }

# Keep main activity and all activities
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.appcompat.app.AppCompatActivity { *; }

# Keep broadcast receivers
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep services
-keep class * extends android.app.Service { *; }

# Keep all composables (functions marked @Composable)
-keepclasseswithmembernames class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ============ MISCELLANEOUS ============
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum values/names
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep custom application classes
-keep class * extends android.app.Application { *; }

# Keep View constructors (for inflation)
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# ============ DEBUG BUILDS ============
# For debug builds, you might want to disable minification:
# Just comment out isMinifyEnabled = true in build.gradle.kts
