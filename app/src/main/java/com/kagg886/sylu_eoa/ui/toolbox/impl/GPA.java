package com.kagg886.sylu_eoa.ui.toolbox.impl;

import android.content.Intent;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.sub_activity.GPAActivity;
import com.kagg886.sylu_eoa.ui.toolbox.Tool;

/**
 * @Author kagg886
 * @Date 2024/1/4 下午9:31
 * @description:
 */

public class GPA implements Tool {
    @Override
    public String getName() {
        return "大创学分";
    }

    @Override
    public int getImageResourceId() {
        return R.drawable.ic_nov;
    }

    @Override
    public Intent callActivity() {
        return new Intent(MainApplication.getApp(), GPAActivity.class);
    }
}
