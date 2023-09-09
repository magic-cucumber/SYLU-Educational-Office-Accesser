package com.kagg886.sylu_eoa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.util.UIUtil;
import org.jsoup.Jsoup;

import java.util.concurrent.CompletableFuture;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.warning);

        TextView v = findViewById(R.id.msg);
        v.setText("正在检测教务网连接性");
        ((ImageView) findViewById(R.id.image)).setImageResource(R.drawable.ic_search);

        CompletableFuture.supplyAsync(() -> {
            //尽最大努力防止教务抽风
            for (int i = 0; i < 10; i++) {
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
                    runOnUiThread(() -> v.setText(v.getEditableText().append(".")));
                    continue;
                }
                return true;
            }
            return false;
        }).thenAccept((bool) -> {
            if (!bool) {
                UIUtil.showToast(LauncherActivity.this, "教务网连接失败");
            }
            Intent i = new Intent(LauncherActivity.this, MainActivity.class);
            startActivity(i);
        });
    }
}