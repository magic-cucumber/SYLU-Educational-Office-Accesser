package com.kagg886.sylu_eoa.ui.toolbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.util.GridItemDecoration;

public class ToolBoxFragment extends Fragment {


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RecyclerView root = new RecyclerView(getContext());
        root.setLayoutManager(new GridLayoutManager(getContext(), 3));
        root.setAdapter(new ToolsAdapter());

        GridItemDecoration gridItemDecoration = new GridItemDecoration(GridLayoutManager.VERTICAL);
        gridItemDecoration.setColor(getContext().getColor(R.color.purple_200));
        root.addItemDecoration(gridItemDecoration);

        return root;
    }

}