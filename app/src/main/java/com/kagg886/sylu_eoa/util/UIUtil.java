package com.kagg886.sylu_eoa.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.ui.exam.TextViewAdapter;

import java.util.List;

/**
 * @author kagg886
 * @date 2023/9/8 18:20
 **/
public class UIUtil {

    public static void showDetailDialog(Context context, String name, List<List<String>> data, int span) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle(name);

        RecyclerView view = new RecyclerView(context);
        GridLayoutManager layoutManager = new GridLayoutManager(context, span);
        view.setLayoutManager(layoutManager);
        GridItemDecoration decoration = new GridItemDecoration(GridLayoutManager.VERTICAL);
        decoration.setColor(context.getColor(R.color.purple_200));
        view.addItemDecoration(decoration);

        TextViewAdapter adapter = new TextViewAdapter(18);

        data.forEach((line) -> line.forEach((col) -> adapter.getStrings().add(col)));
        view.setAdapter(adapter);

        builder.setView(view);
        builder.create().show();
    }
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
