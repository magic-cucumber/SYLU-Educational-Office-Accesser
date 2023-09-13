package com.kagg886.sylu_eoa.ui.exam;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.data.CacheController;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.ExamResult;
import com.kagg886.sylu_eoa.model.Term;
import com.kagg886.sylu_eoa.model.YearAndSemestersPicker;
import com.kagg886.sylu_eoa.util.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_exam, null);
        choose_year = v.findViewById(R.id.fragment_exam_chooseYear);
        choose_term = v.findViewById(R.id.fragment_exam_chooseTerm);

        this.container = v.findViewById(R.id.fragment_exam_container);

        LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);
        CacheController controller = MainApplication.getApp().getConfig("cache", CacheController.class);

        CompletableFuture.supplyAsync(() -> {
            try {
                if (config.getUser() == null) {
                    UIUtil.showToast(getActivity(), "登录后才能执行操作!");
                    return null;
                }
                controller.getPickerBeforeOutOfDate(config.getUser());
                return config.getUser().getExamListByTerm(controller.getSchoolCalenderBeforeOutOfDate(config.getUser()).getCurrentTerm());
            } catch (RuntimeException e) {
                if (e.getCause() instanceof LoginException.CookieOutOfDate) { //检查失败，使用旧课表
                    UIUtil.showToast(getActivity(), "考试项已经过时,请重新登录以拉取最新的缓存!");
                }
                return null;
            }
        }).thenAccept((result) -> {
            if (result == null) {
                return;
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter = new ExamDetailsAdapter(result);
                this.container.setAdapter(adapter);
                this.container.setLayoutManager(new LinearLayoutManager(getContext()));
                this.container.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

                //选定选择器
                YearAndSemestersPicker picker = controller.getPickerBeforeOutOfDate(config.getUser());
                String[] yearArr = picker.getYear().keySet().stream().sorted(Comparator.comparingInt(k -> {
                    try {
                        return -Integer.parseInt(k.split("-")[0]);
                    } catch (Exception e) {
                        return -114514;
                    }
                })).toArray(String[]::new);
                choose_year.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, yearArr));
                for (int i = 0; i < yearArr.length; i++) {
                    if (yearArr[i].equals(picker.getDefaultTerm().getYearsOfSchooling())) {
                        choose_year.setSelection(i);
                    }
                }

                String[] teamArr = picker.getSemester().keySet().toArray(new String[0]);
                choose_term.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, teamArr));

                for (int i = 0; i < teamArr.length; i++) {
                    if (teamArr[i].equals(picker.getDefaultTerm().getSemesterNumber())) {
                        choose_term.setSelection(i);
                    }
                }
            });
        });
        choose_year.setOnItemSelectedListener(this);
        choose_term.setOnItemSelectedListener(this);
        return v;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);
        new Thread(() -> {
            List<ExamResult> info;
            try {
                info = config.getUser().getExamListByTerm(new Term(
                        choose_year.getSelectedItem().toString(),
                        choose_term.getSelectedItem().toString()
                ));
                Log.d(ExamFragment.class.getName(), info.toString());
            } catch (RuntimeException e) {
                if (e.getCause() instanceof LoginException.CookieOutOfDate) { //检查失败，使用旧课表
                    UIUtil.showToast(getActivity(), "凭证失效，请重新登录以使用最新的功能!");
                }
                return;
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.getMValues().clear();
                info.forEach((a) -> adapter.getMValues().add(a));
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
