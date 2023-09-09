package com.kagg886.sylu_eoa.sub_activity.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.kagg886.sylu_eoa.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author kagg886
 * @date 2023/9/8 20:59
 **/
public class DetailsAdapter extends BaseAdapter {
    private final List<Map.Entry<String, String>> data;
    private final Context context;

    public DetailsAdapter(Set<Map.Entry<String, String>> data, Context context) {
        this.data = new ArrayList<>(data);
        this.context = context;
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    @SuppressLint({"ViewHolder", "InflateParams"})
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = LayoutInflater.from(context).inflate(R.layout.adapter_detail, null);
        ((TextView) v.findViewById(R.id.key)).setText(data.get(i).getKey());
        ((TextView) v.findViewById(R.id.value)).setText(data.get(i).getValue());
        return v;
    }
}
