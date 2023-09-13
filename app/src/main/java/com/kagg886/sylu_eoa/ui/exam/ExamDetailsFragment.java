package com.kagg886.sylu_eoa.ui.exam;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.model.ExamResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @projectName: 掌上沈理青春版
 * @package: com.qlstudio.lite_kagg886.fragment
 * @className: ExamFragment
 * @author: kagg886
 * @description: 考试信息
 * @date: 2023/4/14 19:53
 * @version: 1.0
 */
public class ExamFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private Spinner choose_year, choose_term;

    private RecyclerView container;
    private ExamDetailsAdapter adapter;
    private boolean isOnSelecting = false;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_exam, null);
        choose_year = v.findViewById(R.id.fragment_exam_chooseYear);
        choose_term = v.findViewById(R.id.fragment_exam_chooseTerm);

        this.container = v.findViewById(R.id.fragment_exam_container);


//        new Thread(() -> {
//            SyluSession session = GlobalApplication.getApplicationNoStatic().getSession();
//
//            //设置默认UI
//            try {
//                result = session.getExamResult();
//            } catch (OfflineException e) {
//                dialogController.sendEmptyMessage(2);
//                return;
//            }
//            new Handler(Looper.getMainLooper()).post(() -> {
//                adapter = new ExamInfoAdapter(result);
//                this.container.setAdapter(adapter);
//                this.container.setLayoutManager(new LinearLayoutManager(getContext()));
//                this.container.addItemDecoration(new DividerItemDecoration(Objects.requireNonNull(getContext()), DividerItemDecoration.VERTICAL));
//
//                //选定选择器
//                String[] yearArr = result.getYears().keySet().stream().sorted(Comparator.comparingInt(k -> {
//                    try {
//                        return -Integer.parseInt(k.split("-")[0]);
//                    } catch (Exception e) {
//                        return -114514;
//                    }
//                })).toArray(String[]::new);
//                choose_year.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, yearArr));
//                for (int i = 0; i < yearArr.length; i++) {
//                    if (yearArr[i].equals(result.getDefaultYears())) {
//                        choose_year.setSelection(i);
//                    }
//                }
//
//                String[] teamArr = result.getTeamVal().keySet().toArray(new String[0]);
//                choose_term.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, teamArr));
//
//                for (int i = 0; i < teamArr.length; i++) {
//                    if (teamArr[i].equals(result.getDefaultTeamVal())) {
//                        choose_term.setSelection(i);
//                    }
//                }
//            });
//        }).start();
        choose_year.setOnItemSelectedListener(this);
        choose_term.setOnItemSelectedListener(this);
        return v;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (isOnSelecting) {
            return;
        }
        isOnSelecting = true;
        dialogController.sendEmptyMessage(0);
        new Thread(() -> {
            List<ExamResult.ExamInfo> info;
            try {
                info = result.queryResultByYearAndTerm(
                        choose_year.getSelectedItem().toString(),
                        choose_term.getSelectedItem().toString()
                );
                if (GlobalApplication.getApplicationNoStatic().getPreferences().getBoolean("setting_nullfail", false)) {
                    info = info.stream().filter((v) -> v.getStatus() != ExamResult.Status.FUCK_TEACHER).collect(Collectors.toList());
                }
            } catch (OfflineException e) {
                dialogController.sendEmptyMessage(2);
                return;
            }
            updateUI(info);
            dialogController.sendEmptyMessage(1);
            isOnSelecting = false;
        }).start();
    }

    private void updateUI(List<ExamResult.ExamInfo> info) {
        new Handler(Looper.getMainLooper()).post(() -> {
            adapter.getResults().clear();
            info.forEach((a) -> {
                adapter.getResults().add(a);
            });
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
