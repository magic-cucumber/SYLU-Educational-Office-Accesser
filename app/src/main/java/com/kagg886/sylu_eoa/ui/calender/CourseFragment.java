package com.kagg886.sylu_eoa.ui.calender;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.alibaba.fastjson2.JSON;
import com.kagg886.sylu_eoa.MainApplication;
import com.kagg886.sylu_eoa.R;
import com.kagg886.sylu_eoa.SyluUser;
import com.kagg886.sylu_eoa.data.CacheController;
import com.kagg886.sylu_eoa.data.LoginConfig;
import com.kagg886.sylu_eoa.databinding.FragmentCourseBinding;
import com.kagg886.sylu_eoa.model.ClassUnit;
import com.kagg886.sylu_eoa.util.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CourseFragment extends Fragment {

    private boolean isDrag; //判断是否为用户滑动

    private FragmentCourseBinding binding;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        binding = FragmentCourseBinding.inflate(inflater, container, false);
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
            if (controller.getCourse() == null) {
                return 0;
            }
            return 1;
        }).thenApply((i) -> { //有课表数据为1,无课表数据为0
            if (i == 0) {
                if (user == null) {
                    UIUtil.showToast(getActivity(), "请登录以拉取最新的课表缓存!");
                    return null;
                }
                List<ClassUnit> units = user.getClassTableByTerm(user.getSchoolCalender().getCurrentTerm());
                controller.setCourse(units);
                return units;
            }
            //这个阶段应该是异步的
            CompletableFuture.runAsync(() -> {
                if (user.isCookieOutOfDate()) {
                    UIUtil.showToast(getActivity(), "课表可能已经过时,请重新登录以拉取最新的课表缓存!");
                }
            });
            return controller.getCourse();
        }).thenAccept((kb) -> {
            if (kb == null) {
                return;
            }
            Log.i(CourseFragment.class.getName(), JSON.toJSONString(kb));
            UIUtil.showToast(getActivity(), "OK!");
            //在这里编写UI Parse
        });


        return binding.getRoot();
    }


    public static class Empty extends Fragment {
        private final String msg;

        public Empty(String e) {
            this.msg = e;
        }

        @Nullable
        @org.jetbrains.annotations.Nullable
        @Override
        public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.fragment_dashboard, null);
            return v;
        }
    }
}