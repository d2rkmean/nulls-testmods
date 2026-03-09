-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'x'
-flattenpackagehierarchy 'x'
-overloadaggressively
-useuniqueclassmembernames


-renamesourcefileattribute SourceFile
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod


-keep class org.darkmean.testmods.MainActivity { *; }

-keepclassmembers class * extends androidx.activity.ComponentActivity {
    public void onCreate(android.os.Bundle);
}

-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**

-dontwarn kotlin.**
-dontwarn kotlinx.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.** { *; }
-keepnames class okhttp3.** { *; }

-keep class org.json.** { *; }



-keepclassmembers class org.darkmean.testmods.ModItem { *; }
-keep class android.security.** { *; }
-dontwarn android.**


-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
    public static int wtf(...);
}
