-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn androidx.test.platform.app.InstrumentationRegistry

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepattributes LineNumberTable
-allowaccessmodification
-repackageclasses

# MKMB保护
-keep class top.kagg886.mkmb.MMKVInternalLog { *; }


# 保持 META-INF/services 资源文件同步
-adaptresourcefilenames META-INF/services/*
-adaptresourcefilecontents META-INF/services/*

# 保留 InternalServiceModule 接口和所有实现类
-keep interface dev.whyoleg.sweetspi.internal.InternalServiceModule { *; }
-keep class * implements dev.whyoleg.sweetspi.internal.InternalServiceModule { *; }

# 忽略 ServiceLoader 缺失类
-dontwarn io.micrometer.context.**
-dontwarn javax.enterprise.**
-dontwarn reactor.blockhound.**
-dontwarn io.netty.util.internal.logging.**
-dontwarn okhttp3.internal.Util
# 告诉 R8 不检查 META-INF/services 缺失实现
-ignorewarnings
