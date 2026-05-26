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

-keepnames class uk.org.cgatechnologies.wideya.common.data.entities.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.learner_management.entities.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.learner_management.models.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.login.models.*.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.school_group_management.entities.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.school_group_management.models.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.school_management.data.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.school_management.entities.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.school_management.models.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.sync_device.entities.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.sync_device.models.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.teacher_management.entities.* { *; }
-keepnames class uk.org.cgatechnologies.wideya.teacher_management.models.* { *; }

# Keep all HaroldAdmin classes
-keep class com.haroldadmin.cnradapter.** { *; }

# Keep SourceAFIS
#-keep class SecuGen.** { *; }
-keep class com.machinezoo.** { *; }

# Uncomment this to preserve the line number information for
-keepattributes SourceFile,LineNumberTable,InnerClasses
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
   public static ** INSTANCE;
}
-keepclassmembers class <1> {
   public static <1> INSTANCE;
   kotlinx.serialization.KSerializer serializer(...);
}

# For SQLCipher implementation
#-keep,includedescriptorclasses class net.sqlcipher.** { *; }
#-keep,includedescriptorclasses interface net.sqlcipher.** { *; }