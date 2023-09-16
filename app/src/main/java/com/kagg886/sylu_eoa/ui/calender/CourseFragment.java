package com.kagg886.sylu_eoa.ui.calender;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.SyluUser;
import com.kagg886.sylu_eoa.data.CacheController;
import com.kagg886.sylu_eoa.data.ClassTable;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.FragmentCourseBinding;
import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.SchoolCalender;
import com.kagg886.sylu_eoa.util.ItemChooseDialog;
import com.kagg886.sylu_eoa.util.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CourseFragment extends Fragment {

    private boolean isDrag; //判断是否为用户滑动

    private FragmentCourseBinding binding;

    private ContentPagerAdapter adapter;

    private boolean isRefreshing = false;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        binding = FragmentCourseBinding.inflate(inflater, container, false);
        binding.refresh.setOnRefreshListener(this::insertCourse);

        //这里必须传入ChildFragmentManager
        adapter = new ContentPagerAdapter(getChildFragmentManager(), getActivity().getLifecycle());

        binding.content.setAdapter(adapter);
        binding.content.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onPageSelected(int position) {
                binding.broad.setText("第" + (position + 1) + "周(点我切换课表)");
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isDrag = true;
                }
            }
        });
        insertCourse();
        return binding.getRoot();
    }

    private void insertCourse() {
        if (isRefreshing) {
            UIUtil.showToast(requireActivity(), "正在刷新中，请稍后再试");
            return;
        }
        isRefreshing = true;
        binding.refresh.setRefreshing(true);


        LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);
        SyluUser user = config.getUser();

        CacheController controller = MainApplication.getApp().getConfig("cache", CacheController.class);

        CompletableFuture.supplyAsync(() -> {
            if (user == null) {
                UIUtil.showToast(getActivity(), "登录后才能获取最新的课程表缓存!");
            }
            try {
                controller.getSchoolCalenderBeforeOutOfDate(user);
                return controller.getCourseBeforeOutOfDate(user);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof LoginException.CookieOutOfDate) { //检查失败，使用旧课表
                    UIUtil.showToast(getActivity(), "课表可能已经过时,请重新登录以拉取最新的课表缓存!");
                }
                return controller.getCourse();
            }
        }).thenAccept((kb) -> {
            if (kb == null) {
                return;
            }
            //UI Parse

            SchoolCalender calender = controller.getSchoolCalenderBeforeOutOfDate(user);

            int a = 0;
            int currentWeek = -1;
            ClassTable table = new ClassTable(kb);

            ClassTable perWeek;
            LocalDate date = calender.getStart();
            while (!(perWeek = table.queryClassByWeek(a + 1)).isEmpty() || date.isBefore(calender.getEnd())) { //十一假期会空一周，此时会满足perWeek.size()==0但不是最后一周
                if (perWeek.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() -> adapter.getData().add(new Empty("本周无课程!", R.drawable.ic_face)));
                } else {
                    final ClassTable perWeek0 = perWeek;
                    final LocalDate date0 = date;
                    new Handler(Looper.getMainLooper()).post(() -> adapter.getData().add(new CoursePageFragment(perWeek0, date0)));
                }
                a++;
                if (date.isBefore(LocalDate.now()) && date.plusDays(7).isAfter(LocalDate.now())) {
                    currentWeek = a;
                }
                date = date.plusDays(7);
//                break; //仅仅拿第一周做测试
            }

            //设置到正确的周数
            final int len = a;
            List<String> titles = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                titles.add("第" + (i + 1) + "周" + (currentWeek == i + 1 ? "(当前周)" : ""));
            }
            int finalCurrentWeek = currentWeek;
            getActivity().runOnUiThread(() -> {
                binding.broad.setOnClickListener((view0) -> {
                    ItemChooseDialog dialog = new ItemChooseDialog(getActivity());

                    ListView v = new ListView(getActivity());
                    v.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, titles.toArray()));
                    //解决滑动冲突
                    dialog.setContentView(v);
                    v.setOnItemClickListener((parent, view, position, id) -> {
                        if (!isDrag) { //如果是代码操作则切换pager，否则由recycler自行完成
                            binding.content.setCurrentItem(position, false);
                        }
                        isDrag = false;
                        dialog.cancel();
                    });

                    dialog.setContentView(v);
                    dialog.show();
                });
                binding.content.setCurrentItem(finalCurrentWeek - 1, false); //防止一瞬间滑动n次造成的卡顿
            });
        }).exceptionally((e) -> {
            Log.e(CourseFragment.class.getName(), "Read Class Error:", e);
            return null;
        }).thenAccept((p) -> {
            isRefreshing = false;
            requireActivity().runOnUiThread(() -> binding.refresh.setRefreshing(false));
        });
    }


    public static class Empty extends Fragment {
        private final String msg;
        private final int id;

        public Empty(String e, @DrawableRes int id) {
            this.msg = e;
            this.id = id;
        }

        @Nullable
        @org.jetbrains.annotations.Nullable
        @Override
        public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.warning, null);
            ((ImageView) v.findViewById(R.id.image)).setImageResource(id);
            ((TextView) v.findViewById(R.id.msg)).setText(msg);
            return v;
        }
    }
}