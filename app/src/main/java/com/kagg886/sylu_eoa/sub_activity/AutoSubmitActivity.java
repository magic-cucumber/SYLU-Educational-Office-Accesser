package com.kagg886.sylu_eoa.sub_activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.ActivityAsaBinding;
import com.kagg886.sylu_eoa.model.RelatedItem;
import com.kagg886.sylu_eoa.model.RelatedQuestion;
import com.kagg886.sylu_eoa.model.RelatedQuestions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @Author kagg886
 * @Date 2024/1/11 下午4:43
 * @description:
 */

public class AutoSubmitActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAsaBinding binding = ActivityAsaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Handler h = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case 0:
                        binding.textView4.setText("登录完成!正在获取数据...");
                        break;
                    default:
                        binding.progress.setProgress(msg.what, 300);
                        binding.textView4.setText(msg.what + "%");
                }
            }
        };
        LoginConfig c = MainApplication.getApp().getConfig("account", LoginConfig.class);
        CompletableFuture.runAsync(() -> {
            if (c.getUser().isCookieOutOfDate()) {
                c.getUser().loginByPwd(c.getPass());
                c.setUser(c.getUser());
            }
            h.sendEmptyMessage(0);
        }).thenAccept((k) -> {
            List<RelatedItem> items = c.getUser().getUnRelatedItems();
            float max = items.size();
            if (max == 0) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "没有要评价的项目!", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }
            int current = 0;
            for (RelatedItem ri : items) {
                System.out.println(ri);
                RelatedQuestions rq = c.getUser().getRelatedQuestions(ri);
                for (RelatedQuestion relatedQuestion : rq) {
                    System.out.println(relatedQuestion.getDesc());
                    relatedQuestion.select(relatedQuestion.getChoices().get(0));
                    System.out.println("My Choice:" + relatedQuestion.getChoice().getValue());
                }
                c.getUser().submitRelatedQuestion(rq);
                h.sendEmptyMessage((int) ((++current / max) * 100));
            }
        }).thenAccept((v) -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "提交完成!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).exceptionally((a) -> {
            Toast.makeText(this, "发生错误!请携带日志进入官方群反馈", Toast.LENGTH_SHORT).show();
            finish();
            return null;
        });
    }
}
