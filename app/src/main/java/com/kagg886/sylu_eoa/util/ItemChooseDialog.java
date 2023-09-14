package com.kagg886.sylu_eoa.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.kagg886.sylu_eoa.R;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * @author kagg886
 * @date 2023/9/13 21:54
 **/
@Getter
public class ItemChooseDialog extends BottomSheetDialog {

    public ItemChooseDialog(@NonNull @NotNull Context context) {
        super(context, R.style.BottomSheetDialog);
    }


    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        solveTouchConflict(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        solveTouchConflict(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void solveTouchConflict(View v) {
        v.setBackgroundResource(R.drawable.bg_dialog_course);
        traverseViewGroup(v);
    }

    // 遍历viewGroup
    @SuppressLint("ClickableViewAccessibility")
    public int traverseViewGroup(View view) {
        int viewCount = 0;
        if (null == view) {
            return 0;
        }
        if (view instanceof ViewGroup) {
            //遍历ViewGroup,是子view加1，是ViewGroup递归调用
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View child = ((ViewGroup) view).getChildAt(i);
                if (child instanceof ViewGroup) {
                    viewCount += traverseViewGroup(((ViewGroup) view).getChildAt(i));
                } else {
                    viewCount++;
                }
                if (child instanceof ListView) {
                    child.setOnTouchListener((v1, event) -> {
                        //canScrollVertically(-1)的值表示是否能向下滚动，false表示已经滚动到顶部
                        ((ViewGroup) v1).requestDisallowInterceptTouchEvent(v1.canScrollVertically(-1));
                        return false;
                    });
                }
            }
        } else {
            viewCount++;
        }
        return viewCount;
    }
}
