package com.kagg886.sylu_eoa.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author kagg886
 * @date 2023/9/16 19:24
 **/

public class GridItemDecoration extends RecyclerView.ItemDecoration {
    private int mDividerHeight = 2;
    private Paint mPaint;
    private int mOrientation;

    public GridItemDecoration(@RecyclerView.Orientation int orientation) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0xFFFF0000);
        mOrientation = orientation;
    }

    private void setColor(Color color) {
        mPaint.setColor(color.toArgb());
    }

    public void setColor(int color) {
        mPaint.setColor(color);
    }

    @Override
    public void getItemOffsets(@NotNull Rect outRect, View view, RecyclerView parent, @NotNull RecyclerView.State state) {
        GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
        int pos = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();


        int left = 0;
        int top = 0;

        if (pos < layoutManager.getSpanCount()) {
            top = mDividerHeight;
        }
        if (pos % layoutManager.getSpanCount() == 0) {
            left = mDividerHeight;
        }
        outRect.set(left, top, mDividerHeight, mDividerHeight);
    }

    private boolean isFirstRow(GridLayoutManager layoutManager, int itemPosition) {
        GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();
        int spanCount = layoutManager.getSpanCount();
        int spanIndex = spanSizeLookup.getSpanIndex(itemPosition, spanCount);
        int spanSize = spanSizeLookup.getSpanSize(itemPosition);

        if (mOrientation == GridLayoutManager.VERTICAL) {
            return spanIndex == 0;
        } else {
            return (itemPosition % spanCount) == 0;
        }
    }

    private boolean isFirstCol(GridLayoutManager layoutManager, int itemPosition) {
        GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();
        int spanCount = layoutManager.getSpanCount();
        int spanIndex = spanSizeLookup.getSpanIndex(itemPosition, spanCount);
        int spanSize = spanSizeLookup.getSpanSize(itemPosition);

        if (mOrientation == GridLayoutManager.VERTICAL) {
            return (itemPosition % spanCount) == 0;
        } else {
            return spanIndex == 0;
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        c.save();
        int childCount = parent.getChildCount();
        int span = ((GridLayoutManager) Objects.requireNonNull(parent.getLayoutManager())).getSpanCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = parent.getChildAt(i);
            if (i <= span) {
                c.drawRect(childAt.getX(), 0, childAt.getX() + childAt.getWidth() + mDividerHeight, mDividerHeight, mPaint);
            }
            if (i == 0 || i % span == 0) {
                float yStart = childAt.getY();
                if (i == 0) {
                    yStart -= mDividerHeight;
                }
                c.drawRect(0, yStart, mDividerHeight, childAt.getY() + childAt.getHeight() + mDividerHeight, mPaint);
            }
            drawHorizontal(c, childAt);
            drawVertical(c, childAt);
        }
        c.restore();
    }

    public void drawHorizontal(Canvas c, View childAt) {
        int left = childAt.getLeft();
        int right = childAt.getRight();
        int top = childAt.getBottom();
        int bottom = childAt.getBottom() + mDividerHeight;
        c.drawRect(left, top, right, bottom, mPaint);
    }

    public void drawVertical(Canvas c, View childAt) {
        int left = childAt.getRight();
        int right = left + mDividerHeight;
        int top = childAt.getTop();
        int bottom = childAt.getBottom() + mDividerHeight;
        c.drawRect(left, top, right, bottom, mPaint);
    }
}
