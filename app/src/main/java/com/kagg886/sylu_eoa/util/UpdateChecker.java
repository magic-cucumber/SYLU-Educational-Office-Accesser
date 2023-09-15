package com.kagg886.sylu_eoa.util;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.kagg886.sylu_eoa.BuildConfig;
import com.kagg886.sylu_eoa.MainApplication;
import org.jsoup.Jsoup;

import static com.kagg886.sylu_eoa.util.UIUtil.openUrlByBrowser;

/**
 * @author kagg886
 * @date 2023/9/14 22:10
 **/
public class UpdateChecker {

    private static final UpdateChecker INSTANCE = new UpdateChecker();
    private final Handler checkHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Activity ctx = MainApplication.getCurrentActivity();
            switch (msg.what) {
                case 0:
                    JSONObject data = JSONObject.parseObject(msg.getData().getString("update"));
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    String body = data.getString("body");
                    String title = data.getString("tag_name");
                    builder.setTitle(("发现更新:V" + BuildConfig.VERSION_NAME) + "->V" + title);
                    builder.setMessage(body);
                    builder.setPositiveButton("下载", (dialog, which) -> {
                        JSONArray a = data.getJSONArray("assets");
                        for (int i = 0; i < a.size(); i++) {
                            if (a.getJSONObject(i).getString("name").equals("app-release.apk")) {
                                openUrlByBrowser(a.getJSONObject(i).getString("browser_download_url"));
                                ctx.finish();
                                return;
                            }
                        }
                    });
                    builder.setCancelable(false);
                    builder.create().show();
                    break;
                case 1:
                    Toast.makeText(ctx, "当前为最新版本!", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(ctx, "更新检测失败...", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public static UpdateChecker getInstance() {
        return INSTANCE;
    }

    public boolean checkUpdate() {
        try {
            JSONObject object = JSONObject.parseObject(
                    Jsoup.connect("https://gitee.com/api/v5/repos/kagg886/sylu-educational-office-accesser/releases/latest")
                            .ignoreContentType(true)
                            .timeout(10000)
                            .execute().body());
            String newVer = object.getString("tag_name");
            if (!BuildConfig.VERSION_NAME.equals(newVer)) {
                Message message = new Message();
                message.what = 0;
                message.getData().putString("update", object.toString());
                checkHandler.sendMessage(message);
                return true;
            }
            checkHandler.sendEmptyMessage(1);
        } catch (Exception e) {
            checkHandler.sendEmptyMessage(2);
        }
        return false;
    }
}
