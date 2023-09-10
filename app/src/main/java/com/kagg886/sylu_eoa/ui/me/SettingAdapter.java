package com.kagg886.sylu_eoa.ui.me;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.databinding.AdapterSettingBinding;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author kagg886
 * @date 2023/9/10 11:47
 **/
public class SettingAdapter extends BaseAdapter {
    private final List<Item> i = new ArrayList<Item>() {{
        add(new Item(R.drawable.ic_download, "检查更新", (c) -> {
            Toast.makeText(c, "OK!", Toast.LENGTH_LONG).show();
        }));
        add(new Item(R.drawable.ic_download, "检查更新", (c) -> {
            Toast.makeText(c, "OK!", Toast.LENGTH_LONG).show();
        }));
        add(new Item(R.drawable.ic_download, "检查更新", (c) -> {
            Toast.makeText(c, "OK!", Toast.LENGTH_LONG).show();
        }));
        add(new Item(R.drawable.ic_download, "检查更新", (c) -> {
            Toast.makeText(c, "OK!", Toast.LENGTH_LONG).show();
        }));
    }};

    private final Activity c;


    public SettingAdapter(Activity c) {
        this.c = c;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        @SuppressLint("ViewHolder")
        AdapterSettingBinding binding = AdapterSettingBinding.inflate(LayoutInflater.from(c), null, false);
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
