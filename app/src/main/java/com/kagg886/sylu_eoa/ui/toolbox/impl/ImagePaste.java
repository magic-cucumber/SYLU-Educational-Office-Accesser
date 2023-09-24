package com.kagg886.sylu_eoa.ui.toolbox.impl;

import android.content.Intent;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.sub_activity.ImagePasteActivity;
import com.kagg886.sylu_eoa.ui.toolbox.Tool;

/**
 * @author kagg886
 * @date 2023/9/24 11:24
 **/
public class ImagePaste implements Tool {
    @Override
    public String getName() {
        return "图片加水印";
    }

    @Override
    public int getImageResourceId() {
        return R.drawable.ic_image_paste;
    }

    @Override
    public Intent callActivity() {
        LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);

        if (config.getUser() == null) {
            throw new RuntimeException("登录后才能使用'图片加水印'功能");
        }
        String name;
        try {
            name = config.getUser().getProfile().getName();
        } catch (Exception e) {
            if (e instanceof LoginException.CookieOutOfDate) {
                throw new RuntimeException("Cookie失效，请重新登录!");
            }
            throw new RuntimeException(e.getMessage());
        }
        Intent i = new Intent(MainApplication.getCurrentActivity(), ImagePasteActivity.class);
        i.putExtra("stuID", config.getUser().getUserID());
        i.putExtra("name", name);
        return i;
    }
}
