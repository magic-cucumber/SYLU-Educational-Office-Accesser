package com.kagg886.sylu_eoa.ui.toolbox.impl;

import android.content.Intent;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.sub_activity.AutoSubmitActivity;
import com.kagg886.sylu_eoa.ui.toolbox.Tool;

/**
 * @Author kagg886
 * @Date 2024/1/11 下午4:41
 * @description:
 */

public class AutoSubmit implements Tool {
    @Override
    public String getName() {
        return "自动提交期末评价";
    }

    @Override
    public int getImageResourceId() {
        return R.drawable.ic_relate;
    }

    @Override
    public Intent callActivity() {
        return new Intent(MainApplication.getApp(), AutoSubmitActivity.class);
    }
}
