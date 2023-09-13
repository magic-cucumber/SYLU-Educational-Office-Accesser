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
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.SyluUser;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.AdapterExamBinding;
import com.kagg886.sylu_eoa.databinding.WarningBinding;
import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.ExamResult;
import com.kagg886.sylu_eoa.util.UIUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExamDetailsAdapter extends RecyclerView.Adapter<ExamDetailsAdapter.ViewHolder> {

    private final List<ExamResult> mValues;

    public ExamDetailsAdapter(List<ExamResult> items) {
        mValues = items;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            WarningBinding binding = WarningBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            binding.image.setImageResource(R.drawable.ic_search);
            binding.msg.setText("未找到符合要求的结果");
            return new ExamDetailsAdapter.ViewHolder(binding.getRoot());
        }
        return new ViewHolder(AdapterExamBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (holder.status == null) {
            return;
        }
        ExamResult info = mValues.get(position);
        holder.root.setOnClickListener((v -> {
            SyluUser user = MainApplication.getApp().getConfig("account", LoginConfig.class).getUser();
            new Thread(() -> {
                try {
                    List<List<String>> data = user.getInfo(info);
                    MainApplication.getCurrentActivity().runOnUiThread(() -> {
                        showDetailDialog(holder.root.getContext(), info.getName(), data);
                    });
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof LoginException.CookieOutOfDate) {
                        UIUtil.showToast(MainApplication.getCurrentActivity(), "凭证失效，请重新登录!");
                    }
                }
            }).start();
        }));
        holder.className.setText(info.getName());
        holder.teacher.setText(info.getTeacher());
        holder.score.setText(info.getCredit());
        holder.gradePoint.setText(info.getGradePoint());
        holder.scTimeGr.setText(info.getCrTimesGp());

        switch (info.getStatus()) {
            case SUCCESS:
                holder.status.setImageResource(R.drawable.ic_examstatus_success);
                break;
            case SUCCESS_RE:
                holder.status.setImageResource(R.drawable.ic_examstatus_success_re);
                break;
            case FUCK_TEACHER:
                holder.status.setImageResource(R.drawable.ic_examstatus_fuckteacher);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        //在这里进行判断，如果我们的集合的长度为0时，我们就使用emptyView的布局
        if (mValues.isEmpty()) {
            return 0;
        }
        //如果有数据，则使用ITEM的布局
        return 1;
    }

    @Override
    public int getItemCount() {
        if (mValues.isEmpty()) {
            return 1;
        }
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
        data.forEach((line) -> line.forEach((col) -> adapter.getStrings().add(col)));
        view.setAdapter(adapter);

        builder.setView(view);
        builder.create().show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        @Getter
        private View root;
        private ImageView status;
        private TextView teacher, className, score, gradePoint, scTimeGr;

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

        public ViewHolder(View emptyView) {
            super(emptyView);
        }
    }
}