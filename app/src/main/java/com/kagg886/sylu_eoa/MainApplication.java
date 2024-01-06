package com.kagg886.sylu_eoa;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.alibaba.fastjson2.*;
import com.kagg886.sylu_eoa.data.AppSetting;
import com.kagg886.sylu_eoa.util.LogCatcher;
import com.tencent.mmkv.MMKV;
import com.tencent.mmkv.MMKVLogLevel;
import dalvik.system.DexClassLoader;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * 主应用示例，提供全局状态管理
 *
 * @author kagg886
 * @date 2023/9/6 17:23
 **/
@Getter
@SuppressLint("DiscouragedPrivateApi")
public class MainApplication extends Application implements Thread.UncaughtExceptionHandler, Runnable {

    private LogCatcher catcher; //日志抓取器线程

    private final Map<String, Object> configs = new HashMap<>();

    @SuppressLint("PrivateApi")
    public static MainApplication getApp() {
        Application application = null;
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method currentApplicationMethod = atClass.getDeclaredMethod("currentApplication");
            currentApplicationMethod.setAccessible(true);
            application = (Application) currentApplicationMethod.invoke(null);
        } catch (Exception ignored) {
        }
        if (application != null) return (MainApplication) application;
        try {
            Class<?> atClass = Class.forName("android.app.AppGlobals");
            Method currentApplicationMethod = atClass.getDeclaredMethod("getInitialApplication");
            currentApplicationMethod.setAccessible(true);
            application = (Application) currentApplicationMethod.invoke(null);
        } catch (Exception ignored) {
        }
        return (MainApplication) application;
    }

    @SneakyThrows
    public <T> T getConfig(String key, Class<T> tClass) {
        MMKV mmkv = MMKV.defaultMMKV();
        T t = (T) configs.get(key);
        if (t == null) {
            String point = mmkv.decodeString(key, null);
            try { //玄学错误，只能这么写顶着
                t = JSON.parseObject(point, tClass, JSONReader.Feature.FieldBased);
            } catch (JSONException e) {
                t = JSON.parseObject(point, tClass);
            }
            Log.d(MainApplication.class.getName(), "mmkv readObject:" + key + "->" + point);
            if (t == null) {
                t = tClass.getDeclaredConstructor().newInstance();
            }
            for (Method m : tClass.getDeclaredMethods()) {
                m.setAccessible(true);
                if (m.getName().startsWith("set")) {
                    Pine.hook(m, new MethodHook() {
                        @Override
                        public void afterCall(Pine.CallFrame callFrame) {
                            String pz = JSON.toJSONString(callFrame.thisObject, JSONWriter.Feature.IgnoreNonFieldGetter, JSONWriter.Feature.FieldBased);
                            mmkv.encode(key, pz);
                            mmkv.async();
                            Log.d(MainApplication.class.getName(), "mmkv saveObject:" + key + "->" + pz);
                        }
                    });
                }
            }
            configs.put(key, t);
        }
        return t;
    }

    public File getLoggerBase() {
        return new File(getCacheDir(), "log");
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onCreate() {
        super.onCreate();
        MMKV.initialize(this);
        MMKV.setLogLevel(MMKVLogLevel.LevelDebug);
        ISylu.setInstance(new ISyluImpl());
        registerDynamicAOP();
        registerLogCatcher();


        Thread.setDefaultUncaughtExceptionHandler(this);
        new Handler(Looper.getMainLooper()).post(this);
    }

    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi"})
    public static Activity getCurrentActivity() {
        Activity current = null;
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(
                    null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map<?, ?> activities = (Map<?, ?>) activitiesField.get(activityThread);
            for (Object activityRecord : activities.values()) {
                Class<?> activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    current = (Activity) activityField.get(activityRecord);
                }
            }
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
        Log.d(MainApplication.class.getName(), "access getCurrentActivity:" + current);
        return current;
    }

    private void registerLogCatcher() {
        //设置日志记录器
        File logRoot = getLoggerBase();
        logRoot.mkdirs();
        int i = 0;
        File log;
        do {
            LocalDate date = LocalDate.now();
            log = new File(logRoot, String.format("%d-%d-%d_%d.log", date.getYear(), date.getMonth().getValue(), date.getDayOfMonth(), i));
            i++;
        } while (log.exists());
        try {
            log.createNewFile();
            catcher = new LogCatcher(log);
            catcher.start();
        } catch (IOException e) {
            System.out.println(log.getAbsolutePath());
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    //DEBUG模式下闪退
    private void registerDynamicAOP() {
        //对网络请求AOP
        Pine.hook(HttpConnection.class.getMethod("execute"), new MethodHook() {
            @Override
            public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                Connection.Request c = ((Connection) callFrame.thisObject).request();
                Log.d(MainApplication.class.getName(),
                        String.format("prepare executing:%s,data:%s,cookie:%s",
                                c.url(),
                                c.data(),
                                c.header("Cookie")
                        )
                );
            }

            @Override
            public void afterCall(Pine.CallFrame callFrame) {
                if (callFrame.getThrowable() != null) {
                    Log.e(MainApplication.class.getName(), "executing:%s failed", callFrame.getThrowable());
                    return;
                }

                Connection.Response c = ((Connection) callFrame.thisObject).response();

                String body;
                try {
                    body = c.body();
                } catch (Exception e) {
                    Log.e(MainApplication.class.getName(), "executing:%s failed", e);
                    return;
                }
                short bin = 0;
                try {
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

                    for (int i = 0; i < Math.max(500, bytes.length); i++) {
                        char it = (char) bytes[i];
                        if (!Character.isWhitespace(it) && Character.isISOControl(it)) {
                            bin++;
                        }
                        if (bin >= 5) {
                            break;
                        }
                    }
                } catch (Throwable e) {
                    bin = 10;
                }

                Log.d(MainApplication.class.getName(),
                        String.format("executing:%s completed:(%d),%s",
                                c.url(),
                                c.statusCode(),
                                bin == 5 ? "blob" : body
                        )
                );
            }
        });

        CompletableFuture.supplyAsync(() -> {
            try {
                //拉取热补丁
                JSONObject o = JSON.parseObject(
                        Jsoup.connect("https://gitee.com/kagg886/sylu-educational-office-accesser/raw/master-2.0/runtime/hotfix/summary.json")
                                .ignoreContentType(true)
                                .execute()
                                .body()
                );
                //版本不对直接停止拉取
                if (o.getInteger("require") == null || o.getInteger("require") != BuildConfig.VERSION_CODE) {
                    return null;
                }
                AppSetting setting = MainApplication.getApp().getConfig("setting", AppSetting.class);

                File f = new File(getFilesDir(), o.getString("name"));
                if (o.getIntValue("ver") != setting.getHotFixVersion()) {
                    setting.setHotFixVersion(o.getIntValue("ver"));
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    byte[] apk = Jsoup.connect("https://gitee.com/kagg886/sylu-educational-office-accesser/raw/master-2.0/runtime/hotfix/" + o.getString("name"))
                            .ignoreContentType(true)
                            .execute().bodyAsBytes();
                    try (FileOutputStream stream = new FileOutputStream(f)) {
                        stream.write(apk);
                    }
                }

                if (f.exists()) {
                    //String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent
                    DexClassLoader loader = new DexClassLoader(f.getAbsolutePath(), new File(getCacheDir(), "dexOpt").getAbsolutePath(), null, getClassLoader());

                    Class<? extends Callable<Boolean>> clazz = (Class<? extends Callable<Boolean>>) loader.loadClass("com.kagg886.sylu_eoa.hotfix.HotFixExecutor");
                    Boolean rtn = clazz.getDeclaredConstructor().newInstance().call();

                    Log.i(MainApplication.class.getName(), "Hotfix inject status:" + rtn);
                }
            } catch (Throwable e) {
                Log.e(MainApplication.class.getName(), "Hotfix inject failed", e);
            }
            return null;
        });
    }

    @Override
    public void uncaughtException(@NonNull @NotNull Thread t, @NonNull @NotNull Throwable e) {
        goCrash(e);
    }

    @Override
    public void run() {
        try {
            Looper.loop();
        } catch (Throwable th) {
            goCrash(th);
        }
    }

    private void goCrash(Throwable th) {
        Log.e("WRONG", "App Crash and will exit App", th);
        new Thread(() -> {
//            Intent i = new Intent(getApplicationContext(), ErrorActivity.class);
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?
//            i.putExtra("ex", th);
//            startActivity(i);
//            android.os.Process.killProcess(android.os.Process.myPid());
        }).start();
    }
}