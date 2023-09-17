package com.kagg886.sylu_eoa.ui.toolbox.impl;

import android.content.Intent;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.sub_activity.SecondClassActivity;
import com.kagg886.sylu_eoa.ui.toolbox.Tool;

/**
 * @author kagg886
 * @date 2023/9/16 19:37
 **/
public class SecondClass implements Tool {
    @Override
    public String getName() {
        return "第二课堂";
    }

    @Override
    public int getImageResourceId() {
        return R.drawable.ic_face;
    }

    @Override
    public Intent callActivity() {
        LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);

        if (config.getUser() == null) {
            throw new RuntimeException("登录后才能使用'第二课堂'功能");
        }
        if (config.getUser().getUserID() == null) {
            throw new RuntimeException("登录后才能使用'第二课堂'功能");
        }

        Intent i = new Intent(MainApplication.getCurrentActivity(), SecondClassActivity.class);
        i.putExtra("stuID", config.getUser().getUserID());
        return i;
    }
}
