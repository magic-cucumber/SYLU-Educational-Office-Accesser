package com.kagg886.sylu_eoa.ui.exam;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.databinding.AdapterExamBinding;
import com.kagg886.sylu_eoa.model.ExamResult;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ExamDetailsAdapter extends RecyclerView.Adapter<ExamDetailsAdapter.ViewHolder> {

    private final List<ExamResult> mValues;

    public ExamDetailsAdapter(List<ExamResult> items) {
        mValues = items;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {

        return new ViewHolder(AdapterExamBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        ExamResult info = mValues.get(position);
        holder.root.setOnClickListener((v -> {
//            new Thread(() -> {
//                try {
//                    List<List<String>> data = result.queryDetailsByExamInfo(info);
//                    new Handler(Looper.getMainLooper()).post(() -> {
//                        showDetailDialog(holder.root.getContext(), info.getName(), data);
//                    });
//                } catch (OfflineException ignored) {
//                    GlobalApplication.getCurrentActivity().runOnUiThread(() -> Toast.makeText(v.getContext(), "登录状态已过期，请重新登录", Toast.LENGTH_LONG).show());
//                    GlobalApplication.getApplicationNoStatic().logout();
//                }
//            }).start();
        }));
        holder.className.setText(info.getName());
        holder.teacher.setText(info.getTeacher());
        holder.score.setText(info.getCredit());
        holder.gradePoint.setText(info.getGradePoint());
        holder.scTimeGr.setText(info.getCrTimesGp());

//        switch (info.getStatus()) {
//            case SUCCESS:
//                holder.status.setImageResource(R.drawable.ic_examstatus_success);
//                break;
//            case SUCCESS_RE:
//                holder.status.setImageResource(R.drawable.ic_examstatus_success_re);
//                break;
//            case FUCK_TEACHER:
//                holder.status.setImageResource(R.drawable.ic_examstatus_fuckteacher);
//                break;
//        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    private void showDetailDialog(Context context, String name, List<List<String>> data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("课程: '" + name + "' 详细信息");

        RecyclerView view = new RecyclerView(context);
        GridLayoutManager layoutManager = new GridLayoutManager(context, 3);
        view.setLayoutManager(layoutManager);

        TextViewAdapter adapter = new TextViewAdapter(17);

        adapter.getStrings().addAll(Arrays.asList("成绩分项", "成绩分项比例", "成绩"));
        data.forEach((line) -> {
            line.forEach((col) -> {
                adapter.getStrings().add(col);
            });
        });
        view.setAdapter(adapter);

        builder.setView(view);
        builder.create().show();
        builder.create().show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        @Getter
        private final View root;
        private final ImageView status;
        private final TextView teacher, className, score, gradePoint, scTimeGr;

        public ViewHolder(@NotNull AdapterExamBinding binding) {
            super(binding.getRoot());
            this.root = itemView;
            status = binding.examitemStatus;
            teacher = binding.examitemTeacher;
            className = binding.examitemClassname;
            score = binding.examitemScore;
            gradePoint = binding.examitemGradepoint;
            scTimeGr = binding.examitemGdtimesc;
        }

    }
}