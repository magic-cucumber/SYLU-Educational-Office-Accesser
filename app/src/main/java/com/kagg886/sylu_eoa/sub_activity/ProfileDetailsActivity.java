package com.kagg886.sylu_eoa.sub_activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.alibaba.fastjson2.JSON;
import com.kagg886.sylu_eoa.MainActivity;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.model.Profile;
import com.kagg886.sylu_eoa.sub_activity.adapter.DetailsAdapter;
import com.tencent.mmkv.MMKV;

import java.util.LinkedHashMap;

public class ProfileDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_details);
        ListView listView = findViewById(R.id.list);

        Profile f = JSON.parseObject(getIntent().getStringExtra("data"), Profile.class);


        listView.setAdapter(new DetailsAdapter(new LinkedHashMap<String, String>() {{
            put("姓名", f.getName());
            put("学院", f.getCollegeName());
            put("专业", f.getStudyName());
            put("邮箱", f.getEmail());
            put("手机", f.getPhone());
            put("身份证", f.getId());
            put("政治面貌", f.getPolicy());
            put("外语", f.getLanguage());
            put("a", "b");
        }}.entrySet(), this));

        findViewById(R.id.button).setOnClickListener((v) -> {
            MMKV.defaultMMKV().remove("account").apply();
            Intent i = new Intent(ProfileDetailsActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }
}