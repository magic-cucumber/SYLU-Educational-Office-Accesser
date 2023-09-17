package com.kagg886.sylu_eoa.sub_activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.data.SecondClassData;
import com.kagg886.sylu_eoa.databinding.DialogClass2Binding;
import com.kagg886.sylu_eoa.databinding.FragmentClass2Binding;
import com.kagg886.sylu_eoa.util.SpiderWebPropertyDiagram;
import com.kagg886.sylu_eoa.util.UIUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * 第二课堂不属于教务系统，直接排除在api模块外
 *
 * @author kagg886
 * @date 2023/9/17 20:40
 **/
public class SecondClassActivity extends AppCompatActivity {
    private static String[] keys = {
            "A. 思想成长",
            "B. 实践学习",
            "C. 创新创业",
            "D. 志愿公益",
            "E. 文体+技能"
    };
    private FragmentClass2Binding fragmentClass2Binding;
    private DialogClass2Binding dialogClass2Binding;
    private AlertDialog dialog;

    private SecondClassData data;


    private final Handler twLoginResult = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            dialogClass2Binding.login.setEnabled(true);
            dialogClass2Binding.exit.setEnabled(true);
            switch (msg.what) {
                case -1:
                    if (!dialog.isShowing()) {
                        dialog.show();
                    }
                    UIUtil.showToast(SecondClassActivity.this, msg.getData().getString("cause"));
                    break;
                case 0:
                    dialog.cancel();
                    dialog.dismiss();
                    insertData();
                    break;
            }
        }
    };

    private String stuID;


    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stuID = getIntent().getStringExtra("stuID");
        data = MainApplication.getApp().getConfig("SecondClassData", SecondClassData.class);
        fragmentClass2Binding = FragmentClass2Binding.inflate(getLayoutInflater(), null, false);
        dialogClass2Binding = DialogClass2Binding.inflate(getLayoutInflater(), null, false);

        initDialog();

        String cookie = data.getCookie();
        if (TextUtils.isEmpty(cookie)) {
            dialog.show();
        } else {
            new Thread(new FetchData().setCookie(cookie)).start();
        }
    }

    private void initDialog() {
        dialogClass2Binding.tips.setText("使用此功能需要连接校园网。\n团委网初始密码为:SYLU+身份证后六位+!@#\n遗忘密码请寻找本班团支书。");
        dialogClass2Binding.goTW.setOnClickListener(v1 -> UIUtil.openUrlByBrowser("http://xg.sylu.edu.cn/SyluTW/Sys/SystemForm/main.htm"));

        dialogClass2Binding.login.setOnClickListener((v) -> {
            dialogClass2Binding.login.setEnabled(false);
            dialogClass2Binding.exit.setEnabled(false); //我没想到会有人在登录的时候关闭弹窗，然后程序就会闪退。。。
            String pass = dialogClass2Binding.pass.getEditableText().toString();
            //启动数据拉取
            new Thread(new FetchData().setCookie(pass)).start();
        });
        dialogClass2Binding.exit.setOnClickListener((v) -> {
            dialog.cancel();
            dialog.dismiss();
            finish();
        });
        dialog = new AlertDialog.Builder(this)
                .setView(dialogClass2Binding.getRoot())
                .setCancelable(false)
                .create();
    }

    @SuppressLint("DefaultLocale")
    private void insertData() {
        SpiderWebPropertyDiagram diagram = fragmentClass2Binding.class2Diagram;

        HashMap<String, SpiderWebPropertyDiagram.DiagramUnit> map = new HashMap<>();
        for (char i = 'A'; i <= 'E'; i++) {
            double exp; //期望值
            double act; //实际值
            try {
                exp = (double) SecondClassData.class.getMethod("get" + i).invoke(data);
                act = (double) SecondClassData.class.getMethod("get" + i + "1").invoke(data);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            map.put(SecondClassActivity.keys[i - 'A'] + "\n" + "最低达标:" + exp, new SpiderWebPropertyDiagram.DiagramUnit(exp, act));
        }
        diagram.setLabel(map);

        TextView tx = fragmentClass2Binding.class2Text;
        tx.setText(String.format("总分:%.2f/%.2f", data.getSum1(), data.getSum()));

    }

    private static class FetchData implements Runnable {
        private String cookie;

        public FetchData setCookie(String cookie) {
            this.cookie = cookie;
            return this;
        }

        @Override
        public void run() {
            //TODO 拉取数据并填充到data中，成功向Handler发送0，失败发送1
        }
    }
}
