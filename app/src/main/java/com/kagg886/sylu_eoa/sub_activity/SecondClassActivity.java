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
import com.kagg886.sylu_eoa.databinding.ActivityClass2Binding;
import com.kagg886.sylu_eoa.databinding.DialogClass2Binding;
import com.kagg886.sylu_eoa.util.RSA;
import com.kagg886.sylu_eoa.util.SpiderWebPropertyDiagram;
import com.kagg886.sylu_eoa.util.UIUtil;
import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.lang.reflect.InvocationTargetException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 第二课堂不属于教务系统，直接排除在api模块外
 *
 * @author kagg886
 * @date 2023/9/17 20:40
 **/
public class SecondClassActivity extends AppCompatActivity {
    private static final String[] keys = {
            "A. 思想成长",
            "B. 实践学习",
            "C. 创新创业",
            "D. 志愿公益",
            "E. 文体+技能"
    };
    private ActivityClass2Binding activityClass2Binding;
    private DialogClass2Binding dialogClass2Binding;
    private AlertDialog dialog;

    private SecondClassData data;


    private final Handler twFetchResult = new Handler(Looper.getMainLooper()) {
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
        activityClass2Binding = ActivityClass2Binding.inflate(getLayoutInflater(), null, false);
        dialogClass2Binding = DialogClass2Binding.inflate(getLayoutInflater(), null, false);
        setContentView(activityClass2Binding.getRoot());
        initDialog();

        String cookie = data.getCookie();
        if (TextUtils.isEmpty(cookie)) {
            dialog.show();
        } else {
            new Thread(this::fetchData).start();
        }
    }

    @SuppressLint("SetTextI18n")
    private void initDialog() {
        dialogClass2Binding.tips.setText("使用此功能需要连接校园网。\n" +
                "团委网初始密码可能的组合有:" +
                "1. SYLU+身份证后六位+!@#\n" +
                "2. 学号\n" +
                "3. 身份证后六位\n" +
                "遗忘密码请寻找本班团支书。");
        dialogClass2Binding.goTW.setOnClickListener(v1 -> UIUtil.openUrlByBrowser("http://xg.sylu.edu.cn/SyluTW/Sys/SystemForm/main.htm"));

        dialogClass2Binding.login.setOnClickListener((v) -> {
            dialogClass2Binding.login.setEnabled(false);
            dialogClass2Binding.exit.setEnabled(false); //我没想到会有人在登录的时候关闭弹窗，然后程序就会闪退。。。
            String pass = dialogClass2Binding.pass.getEditableText().toString();

            CompletableFuture.supplyAsync(() -> {
                try {
                    data.setCookie(loginTW(pass));
                    fetchData();
                } catch (RuntimeException e) {
                    if (data.getCookie() == null) {
                        Message m = new Message();
                        m.what = -1;
                        m.getData().putString("cause", e.getMessage());
                        twFetchResult.sendMessage(m);
                    } else {
                        UIUtil.showToast(SecondClassActivity.this, "无法获取最新数据，请连接校园网后重试");
                    }
                }
                return null;
            });
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

    @SneakyThrows
    public String loginTW(String pass) {
        Connection.Response resp = Jsoup.connect("http://xg.sylu.edu.cn/SyluTW/Sys/UserLogin.aspx")
                .ignoreContentType(true)
                .execute();
        Document dom = resp.parse();

        String cookie = resp.header("Set-Cookie");

        String sessionID = resp.cookie("ASP.NET_SessionId");

        String pubKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC3hzrH91c0OKgtaSB7GWGfDuUJ" +
                "sMrtiYThDXtJdrCr7exKt2fmIZngoFk71Dv/BPVQCHSuohNNvEV9VVDFSBhsP9xK" +
                "EDAM4/2Lv+wlzN9CuZtLpV3Elo8VacjwMHcjTRmTchRBmijQzZRFrA2LM+qsH3U5" +
                "tRM1uJFbfRMkBq24AwIDAQAB";
        //---------------根据公钥加密-----------------
        resp = Jsoup.connect("http://xg.sylu.edu.cn/SyluTW/Sys/UserLogin.aspx")
                .header("Cookie", cookie)
                .data("UserName", stuID)
                .data("__VIEWSTATE", dom.getElementById("__VIEWSTATE").attr("value"))
                .data("__VIEWSTATEGENERATOR", dom.getElementById("__VIEWSTATEGENERATOR").attr("value"))
                .data("__EVENTVALIDATION", dom.getElementById("__EVENTVALIDATION").attr("value"))
                .data("Password", pass)
                .data("pwd", RSA.getInstance().encrypt(pass, pubKey))
                .data("pubKey", pubKey)
                .data("codeInput", "KHG6")
                .data("queryBtn", "%B5%C7++++++++++%C2%BC")
                .method(Connection.Method.POST).execute();

        dom = resp.parse();
        AtomicBoolean isSuccess = new AtomicBoolean(false);

        dom.getElementsByTag("script").forEach((v) -> {
            if (v.html().startsWith("layer.alert('")) {
                int l = v.html().indexOf("'") + 1;
                int r = v.html().indexOf("'", l);
                throw new IllegalStateException(v.html().substring(l, r));
            }

            if (v.html().equals("window.location.href='SystemForm/main.htm';")) {
                isSuccess.set(true);
            }
        });
        cookie = resp.header("Set-Cookie").split("CenterSoft=")[2].split("; ")[0];
        if (!isSuccess.get()) {
            throw new IllegalStateException();
        }
        return String.format("ASP.NET_SessionId=%s; CenterSoft=%s", sessionID, cookie);
    }

    @SuppressLint("DefaultLocale")
    private void insertData() {
        SpiderWebPropertyDiagram diagram = activityClass2Binding.class2Diagram;

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
            map.put(SecondClassActivity.keys[i - 'A'] + "\n" + "最低达标:" + exp, new SpiderWebPropertyDiagram.DiagramUnit(exp, act, (p) -> {
                //点击条目后弹出详情页面
                UIUtil.showToast(SecondClassActivity.this, "Click:" + p);
            }));
        }
        diagram.setLabel(map);

        TextView tx = activityClass2Binding.class2Text;
        tx.setText(String.format("总分:%.2f/%.2f", data.getSum1(), data.getSum()));

    }

    private void fetchData() {
        try {
            //TODO 拉取数据并填充到data中，成功向Handler发送0，失败发送1
            Document dom = Jsoup.connect("http://xg.sylu.edu.cn/SyluTW/Sys/SystemForm/FinishExam/StuFinishStudentScore.aspx")
                    .header("Cookie", data.getCookie())
                    .timeout(5000)
                    .get();

            for (char a = 'A'; a <= 'E'; a++) {
                Double min = Double.parseDouble(dom.getElementById("Count" + a).text());
                String e = dom.getElementById("Count" + a + "1").text();
                Double now = Double.parseDouble(e.isEmpty() ? "0.00" : e);
                try {
                    SecondClassData.class.getMethod("set" + a, double.class).invoke(data, min);
                    SecondClassData.class.getMethod("set" + a + "1", double.class).invoke(data, now);
                } catch (Exception ignored) {
                }
            }
            data.setSum(Double.parseDouble(dom.getElementById("SunCount").text()));

            String e = dom.getElementById("SunCount1").text();
            data.setSum1(Double.parseDouble(e.isEmpty() ? "0.00" : e));
            twFetchResult.sendEmptyMessage(0);
        } catch (Exception e) {
            if (e instanceof SocketTimeoutException) {
                if (data.getCookie() != null) {
                    UIUtil.showToast(this, "无法连接到团委网，已使用旧数据");
                    twFetchResult.sendEmptyMessage(0);
                }
                return;
            }
            Message m = new Message();
            m.what = -1;
            m.getData().putString("cause", e.getMessage());
            twFetchResult.sendMessage(m);
        }
    }
}
