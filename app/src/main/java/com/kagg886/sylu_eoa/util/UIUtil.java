package com.kagg886.sylu_eoa.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

/**
 * @author kagg886
 * @date 2023/9/8 18:20
 **/
public class UIUtil {
    public static void showDialog(Activity c, String title, String message) {
        c.runOnUiThread(() -> new AlertDialog.Builder(c).setTitle(title).setMessage(message).show());
    }

    public static void showToast(Activity c, String msg) {
        c.runOnUiThread(() -> Toast.makeText(c, msg, Toast.LENGTH_LONG).show());
    }
}
