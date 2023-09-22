package com.kagg886.sylu_eoa.ui.calender;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.model.ClassUnit;
import com.kagg886.sylu_eoa.util.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author kagg886
 * @date 2023/9/9 19:49
 **/
public class ClassTableAdapter extends RecyclerView.Adapter<ClassTableAdapter.TableUnit> {

    @Getter
    private final List<ClassUnit> list = new ArrayList<ClassUnit>() {
        @Override
        public boolean add(ClassUnit classUnit) {
            boolean a = super.add(classUnit);
            notifyItemInserted(size());
            return a;
        }

        @Override
        public ClassUnit remove(int index) {
            ClassUnit a = super.remove(index);
            notifyItemRemoved(index);
            return a;
        }
    };

    @Setter
    private LocalDate date;

    @NonNull
    @NotNull
    @Override
    public TableUnit onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new TableUnit(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_classunit, null));
    }


    public static void click(View v0) {
        ClassUnit u = (ClassUnit) v0.getTag();
        if (u instanceof ClassUnit.Conflict) {
            AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(v0.getContext());
            builder.setTitle("冲突课程详情");

            RecyclerView view = new RecyclerView(v0.getContext());
            view.setLayoutManager(new LinearLayoutManager(v0.getContext(), LinearLayoutManager.HORIZONTAL, false));
            view.setAdapter(new ConflictAdapter(((ClassUnit.Conflict) u).getConflict()));

            builder.setView(view);
            builder.create().show();
//            ((ClassUnit.Conflict) u).getConflict();
            return;
        }
        List<List<String>> lists = new ArrayList<List<String>>() {{
            add(Arrays.asList("节数", u.getLesson().toString()));
            add(Arrays.asList("教室", u.getRoom()));
            add(Arrays.asList("老师", u.getTeacher()));
            add(Arrays.asList("上课时间", u.getWeekEachLesson()));
            add(Arrays.asList("上课周数", u.getWeekAsMinMax()
                    .stream()
                    .map(ClassUnit.Range::formatToString)
                    .collect(Collectors.joining(","))));
        }};
        UIUtil.showDetailDialog(v0.getContext(), u.getName() + "的详细信息", lists, 2);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull @NotNull TableUnit holder, int position) {
        //0 1 2  3  4  5  6  7
        //8 9 10 11 12 13 14 15
        //a mod 8 = 0的是头元素，要丢入时间View
        ClassUnit u = list.get(position);

        if (u == ClassUnit.EMPTY) {
            holder.name.setText(""); //得加个占位，不然有课程表错位bug
            holder.room.setText("");
        }

        if (position == 0) {
            return;
        }

        if (position >= 1 && position <= 7) {
            holder.name.setText("星期" + position);
            holder.room.setText(date.toString());
            holder.rootView.setBackgroundColor(Color.rgb(189, 195, 199));
            if (date.equals(LocalDate.now())) {
                holder.rootView.setBackgroundColor(Color.rgb(127, 140, 141));
            }
            date = date.plusDays(1);
            return;
        }
        if (position % 8 == 0) {
            int k = (position / 8);
            holder.name.setText("第" + k + "节");
            switch (k) {
                case 1: //1-2
                    holder.room.setText("8:00-8:45\n\n8:55-9:40");
                    break;
                case 2://3-4
                    holder.room.setText("10:00-10:45\n\n10:55-11:40");
                    break;
                case 3://5-6
                    holder.room.setText("13:00-13:45\n\n13:55-14:40");
                    break;
                case 4://7-8
                    holder.room.setText("14:50-15:35\n\n15:45-16:30");
                    break;
                case 5://9-10
                    holder.room.setText("16:40-17:25\n\n17:35-18:20");
                    break;
                case 6://11-12
                    holder.room.setText("19:00-19:45\n\n19:55-20:30");
                    break;
            }
            return;
        }
        holder.name.setText(u.getName());
        holder.room.setText(u.getRoom());
        if (u != ClassUnit.EMPTY) {

            //相同课程颜色相同
            Random ran = new Random(holder.name.getText().toString().intern().hashCode());

            int r, g, b;
            r = ran.nextInt(255);
            g = ran.nextInt(255);
            b = ran.nextInt(255);
            holder.rootView.setBackgroundColor(Color.argb(60, r, g, b));
            holder.rootView.setTag(u);
            holder.rootView.setOnClickListener(ClassTableAdapter::click); //复用布局...
        }
    }

    public static class TableUnit extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView room;

        private final View rootView;

        public TableUnit(@NonNull @NotNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.adapter_classunit_class);
            room = itemView.findViewById(R.id.adapter_classunit_room);
            rootView = itemView.findViewById(R.id.adapter_root_view);
        }
    }
}
