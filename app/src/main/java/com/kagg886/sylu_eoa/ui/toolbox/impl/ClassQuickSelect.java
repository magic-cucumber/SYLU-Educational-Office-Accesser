package com.kagg886.sylu_eoa.ui.toolbox.impl;

import android.content.Intent;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.ui.toolbox.Tool;

/**
 * @Author kagg886
 * @Date 2024/1/6 上午12:00
 * @description:
 */

public class ClassQuickSelect implements Tool {
    @Override
    public String getName() {
        return "快速选课";
    }

    @Override
    public int getImageResourceId() {
        return R.drawable.ic_clazz;
    }

    @Override
    public Intent callActivity() {
        return null;
    }
}
