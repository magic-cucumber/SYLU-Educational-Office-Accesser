package com.kagg886.sylu_eoa.ui.toolbox.impl;

import android.content.Intent;
import com.kagg886.sylu_eoa.MainActivity;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.ui.toolbox.Tool;

/**
 * @author kagg886
 * @date 2023/9/16 19:37
 **/
public class PhotoDesign implements Tool {
    @Override
    public String getName() {
        return "测试";
    }

    @Override
    public int getImageResourceId() {
        return R.drawable.ic_face;
    }

    @Override
    public Intent callActivity() {
        return new Intent(MainApplication.getCurrentActivity(), MainActivity.class);
    }
}
