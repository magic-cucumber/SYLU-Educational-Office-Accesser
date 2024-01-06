package com.kagg886.sylu_eoa.sub_activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.ActivityQuickSelectBinding;
import com.kagg886.sylu_eoa.model.SelectableClasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @Author kagg886
 * @Date 2024/1/6 上午11:38
 * @description: 快速选课
 */

public class QuickClassSelectActivity extends AppCompatActivity {

    protected ActivityQuickSelectBinding binding;


    private List<Integer> detected = new ArrayList<>();

    private boolean no8;

    private AlertDialog dialog;

    private ProgressBar bar;

    private Handler dialogHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case 1:
                    dialog.setTitle(msg.getData().getString("title"));
                    break;
                case 2:
                    bar.setProgress(msg.getData().getInt("data"));
                    break;
                case 3:
                    bar.setMax(msg.getData().getInt("data"));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuickSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.chipGroup.setOnCheckedStateChangeListener((chipGroup, list) -> {
            detected = list;
        });

        binding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(QuickClassSelectActivity.this, "该选项暂不支持!", Toast.LENGTH_SHORT).show();
            no8 = isChecked;
        });

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        dialog = new AlertDialog.Builder(this)
                .setTitle("等待中")
                .setCancelable(false)
                .setView(bar)
                .create();
        binding.start.setOnClickListener((v) -> {
            dialog.show();
            setDialogTitle("检查教务网登录情况");
            setAllProgress(2);
            CompletableFuture.runAsync(() -> {
                LoginConfig c = MainApplication.getApp().getConfig("account", LoginConfig.class);
                if (c.getUser() == null) {
                    throw new IllegalStateException("请先登录");
                }
                setCurrentProgress(1);
                if (c.getUser().isCookieOutOfDate()) {
                    c.getUser().loginByPwd(c.getPass());
                }
                setCurrentProgress(2);

                //获取课程列表
                setDialogTitle("获取合适的课程列表");
                setCurrentProgress(0);
                List<SelectableClasses> classes = c.getUser().getSelectableClassList();
                setCurrentProgress(1);

                setDialogTitle("选课中...");
                setCurrentProgress(0);
                setAllProgress(5);

                List<String> checked = new ArrayList<String>() {
                    @Override
                    public boolean add(String s) {
                        if (s == null) {
                            return false;
                        }
                        return super.add(s);
                    }
                };
                //筛选课程列表
                checked.add(selectRandom(classes, R.id.chipInGroup1, "美育模块"));
                setCurrentProgress(1);
                checked.add(selectRandom(classes, R.id.chipInGroup2, "自然科学模块"));
                setCurrentProgress(2);
                checked.add(selectRandom(classes, R.id.chipInGroup3, "人文素质教育模块"));
                setCurrentProgress(3);
                checked.add(selectRandom(classes, R.id.chipInGroup4, "身体心理素质教育模块"));
                setCurrentProgress(4);
                checked.add(selectRandom(classes, R.id.chipInGroup5, "经济管理模块"));
                setCurrentProgress(5);
                dialog.cancel();
                runOnUiThread(() -> {
                    Toast.makeText(this, "已选中下列课程:" + checked, Toast.LENGTH_LONG).show();
                    finish();
                });
            }).exceptionally((ex) -> {
                ex.printStackTrace();
                runOnUiThread(() -> {
                    dialog.cancel();
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
                return null;
            });
        });
    }

    private String selectRandom(List<SelectableClasses> classes, int id, String modules) {
        classes = classes.stream().filter((ev) -> {
            if (detected.contains(id)) { //美育模块
                return ev.getModules().contains(modules);
            }
            return false;
        }).collect(Collectors.toList());
        if (classes.isEmpty()) {
            return null;
        }
        LoginConfig c = MainApplication.getApp().getConfig("account", LoginConfig.class);

        int retry = 0;
        while (retry <= 5) {
            retry++;
            int p = new Random().nextInt(classes.size());
            try {
                c.getUser().select(classes.get(p));
                return classes.get(p).getName();
            } catch (Exception e) {
                if ("超过通识选修课本学期最高选课门次限制，不可选！".equals(e.getMessage())) {
                    return null;
                }
                if ("一门课程只能选一个教学班，不可再选！".equals(e.getMessage())) {
                    return null;
                }
                e.printStackTrace();
            }
        }
        return null;
    }

    private void setDialogTitle(String title) {
        Message m = new Message();
        m.what = 1;
        m.getData().putString("title", title);
        dialogHandler.sendMessage(m);
    }

    private void setCurrentProgress(int min) {
        Message m = new Message();
        m.what = 2;
        m.getData().putInt("data", min);
        dialogHandler.sendMessage(m);
    }

    private void setAllProgress(int max) {
        Message m = new Message();
        m.what = 3;
        m.getData().putInt("data", max);
        dialogHandler.sendMessage(m);
    }
}
