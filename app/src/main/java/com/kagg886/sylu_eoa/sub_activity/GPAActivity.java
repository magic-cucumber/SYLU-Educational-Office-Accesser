package com.kagg886.sylu_eoa.sub_activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.model.GPAScore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author kagg886
 * @Date 2024/1/4 下午9:34
 * @description:
 */

public class GPAActivity extends AppCompatActivity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExpandableListView contain = new ExpandableListView(this);

        setContentView(contain);

//        int width = getWindowManager().getDefaultDisplay().getWidth();
//        contain.setIndicatorBounds(width - 40, width - 10);
        new Thread(() -> {
            LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);

            Map<String, List<GPAScore>> map = config.getUser().getGPAs();
            double sum = map.entrySet()
                    .stream()
                    .flatMap(entry -> entry.getValue().stream())
                    .map(GPAScore::getScore)
                    .mapToDouble(Double::parseDouble)
                    .sum();

            runOnUiThread(() -> {
                contain.setAdapter(new GPAAdapter(map));
                getSupportActionBar().setTitle("总大创学分:" + sum);
                for (int i = 0; i < contain.getExpandableListAdapter().getGroupCount(); i++) {
                    contain.expandGroup(i); //默认展开所有项
                }
            });
        }).start();
    }


    public static class GPAAdapter extends BaseExpandableListAdapter {

        public Map<String, List<GPAScore>> map;

        public GPAAdapter(Map<String, List<GPAScore>> map) {
            this.map = map;
        }

        @Override
        public int getGroupCount() {
            return map.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return ((List<GPAScore>) getGroup(groupPosition)).size();
        }

        @Override
        public Object getGroup(int groupPosition) { //返回值为List<GPAScore>
            return map.get(new ArrayList<>(map.keySet()).get(groupPosition));
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return ((List<GPAScore>) getGroup(groupPosition)).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return getGroup(groupPosition).hashCode();
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return getChild(groupPosition, childPosition).hashCode();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setTextColor(Color.BLACK);
            view.setTextSize(20);
            view.setText(new ArrayList<>(map.keySet()).get(groupPosition));
            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_bi_score, null);


            GPAScore item = (GPAScore) getChild(groupPosition, childPosition);
            ((TextView) view.findViewById(R.id.adapter_bi_name)).setText(item.getName());
            ((TextView) view.findViewById(R.id.adapter_bi_score)).setText(String.format("%.1f", Double.parseDouble(item.getScore())));
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
