package com.kagg886.sylu_eoa.ui.exam;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kagg886.sylu_eoa.MainActivity;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.data.CacheController;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.DialogYearChooserBinding;
import com.kagg886.sylu_eoa.databinding.FragmentExamBinding;
import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.ExamResult;
import com.kagg886.sylu_eoa.model.Term;
import com.kagg886.sylu_eoa.model.YearAndSemestersPicker;
import com.kagg886.sylu_eoa.util.ItemChooseDialog;
import com.kagg886.sylu_eoa.util.NoRecycleArrayAdapter;
import com.kagg886.sylu_eoa.util.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @projectName: 掌上沈理青春版
 * @package: com.qlstudio.lite_kagg886.fragment
 * @className: ExamFragment
 * @author: kagg886
 * @description: 考试信息
 * @date: 2023/4/14 19:53
 * @version: 1.0
 */
public class ExamFragment extends Fragment {

    private FragmentExamBinding binding;

    private RecyclerView container;
    private ExamDetailsAdapter adapter;
    private boolean isRefreshing = false;

    @SuppressLint("NotifyDataSetChanged")
    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        binding = FragmentExamBinding.inflate(inflater, null, false);
        this.container = binding.fragmentExamContainer;
        binding.refresh.setOnRefreshListener(this::insertExamData);

        insertExamData();

        return binding.getRoot();
    }

    private void insertExamData() {
        if (isRefreshing) {
            UIUtil.showToast(requireActivity(), "正在获取考试信息，请等待");
            return;
        }

        isRefreshing = true;
        LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);
        CacheController controller = MainApplication.getApp().getConfig("cache", CacheController.class);

        CompletableFuture.supplyAsync(() -> {
            try {
                if (config.getUser() == null) {
                    UIUtil.showToast(requireActivity(), "登录后才能执行操作!");
                    return null;
                }


                controller.getPickerBeforeOutOfDate(config.getUser());
                return config.getUser().getExamListByTerm(controller.getSchoolCalenderBeforeOutOfDate(config.getUser()).getCurrentTerm());
            } catch (RuntimeException e) {
                if (e.getCause() instanceof LoginException.CookieOutOfDate) { //检查失败，使用旧课表
                    UIUtil.showToast(requireActivity(), "考试项已经过时,请重新登录以拉取最新的缓存!");
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
                ((MainActivity) requireActivity()).getSupportActionBar().setTitle(String.format("考试(%s,第%s学期)", picker.getDefaultTerm().getYearsOfSchooling(), picker.getDefaultTerm().getSemesterNumber()));

                ItemChooseDialog dialog = new ItemChooseDialog(requireActivity());

                DialogYearChooserBinding binding1 = DialogYearChooserBinding.inflate(LayoutInflater.from(requireContext()), null, false);
                dialog.setContentView(binding1.getRoot());

                AtomicReference<String> select_year = new AtomicReference<>();
                registerListView(binding1.pickerYear, picker.getYear().keySet().stream().sorted(Comparator.comparingInt(k -> {
                    try {
                        return -Integer.parseInt(k.split("-")[0]);
                    } catch (Exception e) {
                        return -114514;
                    }
                })).toArray(String[]::new), (pos) -> {
                    select_year.set((String) binding1.pickerYear.getAdapter().getItem(pos));
                });

                AtomicReference<String> select_sem = new AtomicReference<>();
                registerListView(binding1.pickerSem, picker.getSemester().keySet().toArray(new String[0]), (pos) -> {
                    select_sem.set((String) binding1.pickerSem.getAdapter().getItem(pos));
                });

                binding1.btnConfirm.setOnClickListener((v) -> {
                    dialog.cancel();
                    dialog.dismiss();

                    new Thread(() -> {
                        List<ExamResult> info;
                        try {
                            info = config.getUser().getExamListByTerm(new Term(select_year.get(), select_sem.get()));
                            requireActivity().runOnUiThread(() -> {
                                ((MainActivity) requireActivity()).getSupportActionBar().setTitle(String.format("考试(%s,第%s学期)", select_year.get(), select_sem.get()));
                            });
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
                });
                binding.year.setOnClickListener((v0) -> {
                    requireActivity().runOnUiThread(dialog::show);
                });
            });
        }).exceptionally((ex) -> {
            if (ex.getCause() instanceof LoginException.CookieOutOfDate) { //检查失败，使用旧课表
                UIUtil.showToast(getActivity(), "请重新登录以拉取最新的缓存!");
                return null;
            }
            Log.e(ExamFragment.class.getName(), "Get Exam Wrong!", ex);
            UIUtil.showToast(requireActivity(), "获取成绩出现了未知错误，请查看日志");
            return null;
        }).thenAccept((v) -> {
            isRefreshing = false;
            requireActivity().runOnUiThread(() -> binding.refresh.setRefreshing(false));
        });
    }

    private void registerListView(ListView list, String[] str, Consumer<Integer> onClick) {
        AtomicReference<View> chooseView1 = new AtomicReference<>();
        list.setAdapter(new NoRecycleArrayAdapter<>(getActivity(), str));
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (chooseView1.get() != null) {
                chooseView1.get().setBackground(null);
            }
            view.setBackgroundResource(R.color.purple_200);
            chooseView1.set(view);
            onClick.accept(position);
        });
    }
}
