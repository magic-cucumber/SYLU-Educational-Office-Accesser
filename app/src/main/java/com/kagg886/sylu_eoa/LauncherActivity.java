package com.kagg886.sylu_eoa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.kagg886.sylu_eoa.data.AppSetting;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.util.UIUtil;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.warning);

        TextView v = findViewById(R.id.msg);
        v.setText("正在检测教务网连接性");
        ((ImageView) findViewById(R.id.image)).setImageResource(R.drawable.ic_search);

        CompletableFuture<Void> taskCheckBroadCast = CompletableFuture.supplyAsync(() -> {

            CountDownLatch latch = new CountDownLatch(1);
            try {
                String body = Jsoup.connect("https://gitee.com/kagg886/sylu-educational-office-accesser/raw/master-2.0/runtime/broadcast.txt").execute().body();

                AppSetting setting = MainApplication.getApp().getConfig("setting", AppSetting.class);

                if (!body.equals(setting.getBroadCast())) {
                    setting.setBroadCast(body);

                    runOnUiThread(() -> new AlertDialog.Builder(LauncherActivity.this)
                            .setTitle("公告")
                            .setMessage(body)
                            .setPositiveButton("确定", (dialog1, which) -> latch.countDown())
                            .setOnCancelListener(dialog12 -> latch.countDown())
                            .show());
                } else {
                    latch.countDown();
                }
            } catch (IOException e) {
                UIUtil.showToast(LauncherActivity.this, "公告拉取失败...");
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        CompletableFuture<Void> taskCheckAvailable = CompletableFuture.supplyAsync(() -> {
            //尽最大努力防止教务抽风
            for (int i = 0; i < 5; i++) {
                try {
                    LoginConfig c = MainApplication.getApp().getConfig("account", LoginConfig.class);
                    if (c.getUser() == null) {
                        Jsoup.connect("https://jxw.sylu.edu.cn/xtgl/login_slogin.html")
                                .timeout(50000)
                                .get();
                    } else {
                        c.getUser().getProfile();
                    }
                } catch (Exception ignored) {
                    runOnUiThread(() -> v.setText(String.format("%s.", v.getText())));
                    continue;
                }
                return true;
            }
            return false;
        }).thenAccept((bool) -> {
            if (!bool) {
                UIUtil.showToast(LauncherActivity.this, "教务网连接失败");
            }
        }).exceptionally((ex) -> {
            Log.e(LauncherActivity.class.getName(), "Check SYLU failed:", ex);
            UIUtil.showToast(LauncherActivity.this, "发生了未知错误");
            return null;
        });

        CompletableFuture.allOf(taskCheckAvailable, taskCheckBroadCast).thenAccept((p) -> {
            Intent i = new Intent(LauncherActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }
}