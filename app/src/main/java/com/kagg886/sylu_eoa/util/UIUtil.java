package com.kagg886.sylu_eoa.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.kagg886.sylu_eoa.MainApplication;

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

    public static void openUrlByBrowser(String s) {
        Uri uri = Uri.parse(s);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainApplication.getApp().startActivity(i);
    }
}
