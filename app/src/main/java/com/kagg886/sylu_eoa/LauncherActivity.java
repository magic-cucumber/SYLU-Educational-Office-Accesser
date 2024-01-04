package com.kagg886.sylu_eoa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.kagg886.sylu_eoa.data.AppSetting;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.ActivityLaunchBinding;
import com.kagg886.sylu_eoa.util.UIUtil;
import com.kagg886.sylu_eoa.util.UpdateChecker;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLaunchBinding binding = ActivityLaunchBinding.inflate(getLayoutInflater(), null, false);
        setContentView(binding.getRoot());


        binding.include.msg.setText("正在检测教务网连接性");
        binding.include.image.setImageResource(R.drawable.ic_search);

        RotateAnimation rotateAnimation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(500);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        binding.include.image.startAnimation(rotateAnimation);

        binding.text.setText(getOneSentence());

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
            for (int i = 0; i < 3; i++) {
                try {
                    LoginConfig c = MainApplication.getApp().getConfig("account", LoginConfig.class);
                    if (c.getId() != null && c.getPass() != null) {
                        SyluUser user = SyluUser.createUser(c.getId());
                        user.loginByPwd(c.getPass());
                        c.setUser(user);
                    } else {
                        Jsoup.connect("https://jxw.sylu.edu.cn/xtgl/login_slogin.html")
                                .timeout(50000)
                                .get();
                        return true;
                    }
                } catch (Exception ignored) {
                    runOnUiThread(() -> binding.include.msg.setText(String.format("%s.", binding.include.msg.getText())));
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

        CompletableFuture<Void> taskCheckUpdate = CompletableFuture.supplyAsync(() -> {
            CountDownLatch latch = new CountDownLatch(1);

            if (!UpdateChecker.getInstance().checkUpdate()) {
                latch.countDown(); //没更新就打破死循环
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        CompletableFuture.allOf(
                taskCheckAvailable,
                taskCheckBroadCast
                , taskCheckUpdate
        ).thenAccept((p) -> {
            Intent i = new Intent(LauncherActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }

    private String getOneSentence() {
        String[] choose = new String[]{
                "你所热爱的，就是你的生活",
                "如果你以为用户是白痴，那就只有白痴才用它",
                "因！特！耐！特！呀！咩！咯！",
                "幻想乡里没有bus吖~\\(≥▽≤)/~",
                "现在是，幻想时间！",
                "我们是穿梭在银河的火箭队！白洞，白色的明天在等着我们！",
                "逸一时，误一世。逸久逸久罢逸龄。",
                "人生三大错觉，我能反杀，下一发能出金，她喜欢我。",
                "典孝急蚌乐 赢润麻寄摆",

                "git push -f",
                "没有bug的代码是不完美的！",
                "rm rf /*",
                "java.lang.NullPointerException",
                "一名顾客点了一份炒饭，酒吧炸了",
                "JavaScript 总是很有趣的",
                "上帝说要有光，于是就有了光。上帝说，hello world 于是就有了世界。",
                "你的用户永远不会按照你的想法使用产品",

                "你知道吗: SYLU-EOA是开放源代码的免费软件",
                "你知道吗: 目前没有人为这个开源项目赞助小钱钱",
                "你知道吗: 一代SYLU-EOA的工程名称和包名纪念了一个工作室",
                "你知道吗: 开发者由于电脑内存不够被迫使用真机调试而不是内置AVD",
                "你知道吗: 开发者有贴贴瘾",
        };

        return choose[new Random().nextInt(choose.length)];
    }
}