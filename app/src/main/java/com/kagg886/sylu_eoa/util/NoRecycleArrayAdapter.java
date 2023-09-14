package com.kagg886.sylu_eoa.util;

import android.content.Context;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;

/**
 * 防止布局复用的View，大数据请勿使用
 *
 * @author kagg886
 * @date 2023/9/14 13:15
 **/
public class NoRecycleArrayAdapter<T> extends ArrayAdapter<T> {
    public NoRecycleArrayAdapter(@NonNull Context context, T[] t) {
        super(context, android.R.layout.simple_list_item_1, t);
    }

    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
