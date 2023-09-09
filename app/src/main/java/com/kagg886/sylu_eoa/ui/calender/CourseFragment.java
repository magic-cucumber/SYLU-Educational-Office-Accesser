package com.kagg886.sylu_eoa.ui.calender;

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
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.alibaba.fastjson2.JSON;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.SyluUser;
import com.kagg886.sylu_eoa.data.CacheController;
import com.kagg886.sylu_eoa.data.ClassTable;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.FragmentCourseBinding;
import com.kagg886.sylu_eoa.exception.LoginException;
import com.kagg886.sylu_eoa.model.ClassUnit;
import com.kagg886.sylu_eoa.model.SchoolCalender;
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

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        binding = FragmentCourseBinding.inflate(inflater, container, false);
        adapter = new ContentPagerAdapter(getActivity());
        binding.content.setAdapter(adapter);
        binding.content.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onPageSelected(int position) {
                binding.counter.setSelection(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isDrag = true;
                }
            }
        });
        LoginConfig config = MainApplication.getApp().getConfig("account", LoginConfig.class);
        SyluUser user = config.getUser();

        CacheController controller = MainApplication.getApp().getConfig("cache", CacheController.class);


        CompletableFuture.supplyAsync(() -> {
            List<ClassUnit> units = controller.getCourse();
            if (units == null) {
                if (user == null) { //无课表，未登录
                    UIUtil.showToast(getActivity(), "请登录以拉取最新的课表缓存!");
                    return null;
                }
                //无课表，已登录
                SchoolCalender calender = user.getSchoolCalender();
                controller.setCalender(calender);
                controller.setCourseOutOfDateTimeStamp(System.currentTimeMillis() + 604800000L);

                units = user.getClassTableByTerm(calender.getCurrentTerm());
                controller.setCourse(units);
                return units;
            }
            //有课表，先检查缓存
            if (System.currentTimeMillis() - controller.getCourseOutOfDateTimeStamp() > 0) { //缓存过期，拉取最新课表
                try {
                    units = user.getClassTableByTerm(user.getSchoolCalender().getCurrentTerm());
                    controller.setCourse(units);
                    controller.setCourseOutOfDateTimeStamp(System.currentTimeMillis() + 604800000L); //7天刷新一次
                    return units;
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof LoginException.CookieOutOfDate) { //检查失败，使用旧课表
                        UIUtil.showToast(getActivity(), "课表可能已经过时,请重新登录以拉取最新的课表缓存!");
                        return units;
                    }
                }
            }
            //缓存未过期，直接使用读取的课表缓存
            return controller.getCourse();
        }).thenAccept((kb) -> {
            if (kb == null) {
                return;
            }
            Log.i(CourseFragment.class.getName(), JSON.toJSONString(kb));
            //在这里编写UI Parse

            SchoolCalender calender;
            if (System.currentTimeMillis() - controller.getCourseOutOfDateTimeStamp() > 0) {
                calender = user.getSchoolCalender();
                controller.setCalender(calender);
                controller.setCourseOutOfDateTimeStamp(System.currentTimeMillis() + 604800000L);
            } else {
                calender = controller.getCalender();
            }

            int a = 0;
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
                date = date.plusDays(7);
//                break; //仅仅拿第一周做测试
            }

            //设置到正确的周数
            final int len = a;
            new Handler(Looper.getMainLooper()).post(() -> {
                List<String> titles = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    titles.add("第" + (i + 1) + "周");
                }
                binding.counter.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, titles.toArray()));
                binding.counter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (!isDrag) { //如果是代码操作则切换pager，否则由recycler自行完成
                            binding.content.setCurrentItem(position, false);
                        }
                        isDrag = false;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                LocalDate now = LocalDate.now();
                int index = (now.getDayOfYear() - calender.getStart().getDayOfYear()) / 7;
                binding.content.setCurrentItem(index, false); //防止一瞬间滑动n次造成的卡顿
            });
        }).exceptionally((e) -> {
            Log.e(CourseFragment.class.getName(), "Read Class Error:", e);
            return null;
        });


        return binding.getRoot();
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