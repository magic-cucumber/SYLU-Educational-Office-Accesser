package com.kagg886.sylu_eoa.ui.calender;

import android.graphics.Color;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.databinding.AdapterClassunitBinding;
import com.kagg886.sylu_eoa.model.ClassUnit;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * @author kagg886
 * @date 2023/9/22 16:02
 **/
@AllArgsConstructor
public class ConflictAdapter extends RecyclerView.Adapter<ConflictAdapter.ConflictHolder> {

    private final List<ClassUnit> data;


    @NonNull
    @NotNull
    @Override
    public ConflictHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        AdapterClassunitBinding binding = AdapterClassunitBinding.inflate(MainApplication.getCurrentActivity().getLayoutInflater(), null, false);
        return new ConflictHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ConflictHolder holder, int position) {
        ClassUnit u = data.get(position);

        holder.binding.adapterClassunitClass.setText(u.getName());
        holder.binding.adapterClassunitRoom.setText(u.getRoom());

        //相同课程颜色相同
        Random ran = new Random(holder.binding.adapterClassunitClass.getText().toString().intern().hashCode());

        int r, g, b;
        r = ran.nextInt(255);
        g = ran.nextInt(255);
        b = ran.nextInt(255);
        holder.binding.adapterRootView.setBackgroundColor(Color.argb(60, r, g, b));
        holder.binding.adapterRootView.setTag(u);
        holder.binding.adapterRootView.setOnClickListener(ClassTableAdapter::click); //复用布局...
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    public static class ConflictHolder extends RecyclerView.ViewHolder {

        private final AdapterClassunitBinding binding;

        public ConflictHolder(@NonNull @NotNull AdapterClassunitBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
