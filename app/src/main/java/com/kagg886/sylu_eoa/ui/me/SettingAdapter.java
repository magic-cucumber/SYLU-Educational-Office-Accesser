package com.kagg886.sylu_eoa.ui.me;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.databinding.AdapterSettingBinding;
import com.kagg886.sylu_eoa.util.UIUtil;
import lombok.Data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author kagg886
 * @date 2023/9/10 11:47
 **/
public class SettingAdapter extends BaseAdapter {
    private final List<Item> i = new ArrayList<Item>() {{


        add(new Item(R.drawable.ic_money, "赞助我", (c) -> {
            ImageView view = new ImageView(c);
            try {
                view.setImageBitmap(BitmapFactory.decodeStream(c.getAssets().open("pay.png")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            new AlertDialog.Builder(c).setTitle("截图保存二维码以捐赠").setView(view).create().show();
        }));

        add(new Item(R.drawable.ic_bug, "分享运行日志", (c) -> {
            new AlertDialog.Builder(c).setTitle("警告").setIcon(R.drawable.ic_warn).setMessage("日志中会含有教务凭证，提供给不信任人员可能会导致隐私泄露\n" + "请确保你要这么做!").setPositiveButton("确定", (dialog, which) -> {
                File base = MainApplication.getApp().getLoggerBase();

                CompletableFuture.supplyAsync(() -> {
                    File target = new File(c.getCacheDir(), "share.zip");
                    if (target.exists()) {
                        target.delete();
                    }
                    try {
                        target.createNewFile();
                        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target.toPath()))) {
                            for (File log : Objects.requireNonNull(base.listFiles())) {
                                ZipEntry entry = new ZipEntry(log.getName());
                                out.putNextEntry(entry);
                                try (FileInputStream stream = new FileInputStream(log)) {
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = stream.read(buffer)) != -1) {
                                        out.write(buffer, 0, len);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return target;
                }).thenAccept(target -> {
                    if (target == null) {
                        UIUtil.showToast(MainApplication.getCurrentActivity(), "分享失败...");
                        return;
                    }
                    Intent intent = new Intent("android.intent.action.SEND");
                    intent.putExtra("android.intent.extra.STREAM",
                            FileProvider.getUriForFile(
                                    c,
                                    "com.kagg886.sylu_eoa.fileprovider",
                                    target
                            )
                    );
                    intent.setType("*/*");
                    c.startActivity(intent);
                }).exceptionally((ex) -> {
                    Log.e(SettingAdapter.class.getName(), "Share Log Failed", ex);
                    UIUtil.showToast(MainApplication.getCurrentActivity(), "日志获取失败...");
                    return null;
                });
            }).show();
        }));
    }};

    private final Activity c;


    public SettingAdapter(Activity c) {
        this.c = c;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        @SuppressLint("ViewHolder") AdapterSettingBinding binding = AdapterSettingBinding.inflate(LayoutInflater.from(c), null, false);
        Item i = this.i.get(position);
        binding.textView.setText(i.name);
        binding.imageView.setImageResource(i.res);
        binding.getRoot().setOnClickListener(i.listener);
        return binding.getRoot();
    }

    @Override
    public int getCount() {
        return i.size();
    }

    @Override
    public Object getItem(int position) {
        return i.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Data
    public class Item {
        private String name;
        private View.OnClickListener listener;
        private int res;

        public Item(int res, String name, Consumer<Context> listener) {
            this.name = name;
            this.res = res;
            this.listener = (v) -> c.runOnUiThread(() -> listener.accept(c));
        }
    }
}
