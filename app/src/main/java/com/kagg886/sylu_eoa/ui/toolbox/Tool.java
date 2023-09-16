package com.kagg886.sylu_eoa.ui.toolbox;

import android.content.Intent;

/**
 * @author kagg886
 * @date 2023/9/16 19:33
 **/
public interface Tool {
    String getName();

    int getImageResourceId();

    Intent callActivity();
}
