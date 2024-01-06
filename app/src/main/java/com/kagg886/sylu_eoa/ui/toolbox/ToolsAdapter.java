package com.kagg886.sylu_eoa.ui.toolbox;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.ui.toolbox.impl.ClassQuickSelect;
import com.kagg886.sylu_eoa.ui.toolbox.impl.GPA;
import com.kagg886.sylu_eoa.ui.toolbox.impl.ImagePaste;
import com.kagg886.sylu_eoa.ui.toolbox.impl.SecondClass;
import com.kagg886.sylu_eoa.util.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @projectName: 掌上沈理青春版
 * @package: com.qlstudio.lite_kagg886.adapter
 * @className: ClassTableAdapter
 * @author: kagg886
 * @description: 装载课程表条目的适配器，横向装配
 * @date: 2023/5/15 10:43
 * @version: 1.0
 */
public class ToolsAdapter extends RecyclerView.Adapter<ToolsAdapter.TableUnit> {

    private static final List<Tool> list = new ArrayList<Tool>() {{
        add(new SecondClass());
        add(new ImagePaste());
        add(new ClassQuickSelect());
        add(new GPA());
    }};

    @NonNull
    @NotNull
    @Override
    public TableUnit onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new TableUnit(LayoutInflater.from(parent.getContext()).inflate(R.layout.warning, null));
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull @NotNull TableUnit holder, int position) {
        holder.txt.setText(list.get(position).getName());
        holder.img.setImageResource(list.get(position).getImageResourceId());
        holder.root.setOnClickListener((a) ->
                CompletableFuture.runAsync(() ->
                        MainApplication.getCurrentActivity().startActivity(list.get(position).callActivity())
                ).exceptionally((e -> {
                    //TODO 未测试
                    UIUtil.showToast(MainApplication.getCurrentActivity(), e.getCause().getMessage());
                    return null;
                })));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class TableUnit extends RecyclerView.ViewHolder {
        public final ImageView img;
        public final TextView txt;

        private final View root;

        public TableUnit(@NonNull @NotNull View itemView) {
            super(itemView);
            this.root = itemView;
            img = itemView.findViewById(R.id.image);
            txt = itemView.findViewById(R.id.msg);
        }
    }
}

