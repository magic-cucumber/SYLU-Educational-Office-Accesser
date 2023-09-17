package com.kagg886.sylu_eoa.sub_activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alibaba.fastjson2.JSON;
import com.kagg886.sylu_eoa.MainActivity;
import com.kagg886.sylu_eoa.databinding.ActivityProfileDetailsBinding;
import com.kagg886.sylu_eoa.model.Profile;
import com.kagg886.sylu_eoa.ui.exam.TextViewAdapter;
import com.kagg886.sylu_eoa.util.GridItemDecoration;
import com.tencent.mmkv.MMKV;

import java.util.Arrays;

public class ProfileDetailsActivity extends AppCompatActivity {

    private ActivityProfileDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityProfileDetailsBinding.inflate(getLayoutInflater(), null, false);
        setContentView(binding.getRoot());

        Profile f = JSON.parseObject(getIntent().getStringExtra("data"), Profile.class);


        RecyclerView view = binding.list;
        view.setLayoutManager(new GridLayoutManager(this, 2));
        view.addItemDecoration(new GridItemDecoration(GridLayoutManager.VERTICAL));
        TextViewAdapter adapter = new TextViewAdapter(18);

        adapter.getStrings().addAll(Arrays.asList(
                "姓名", f.getName(),
                "学院", f.getCollegeName(),
                "专业", f.getStudyName(),
                "邮箱", f.getEmail(),
                "手机", f.getPhone(),
                "身份证", f.getId(),
                "政治面貌", f.getPolicy(),
                "外语", f.getLanguage()
        ));

        view.setAdapter(adapter);


        binding.button.setOnClickListener((v) -> {
            MMKV.defaultMMKV().remove("account").apply();
            Intent i = new Intent(ProfileDetailsActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }
}